package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.agent.runtime.decision.DecisionType;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.tool.AgentToolContext;
import com.selfinvestor.earnings.agent.runtime.tool.ToolExecutor;
import com.selfinvestor.earnings.agent.runtime.tool.ToolResult;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemType;
import com.selfinvestor.earnings.agent.runtime.turn.TurnManager;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 执行 LLM Decide、Tool Call、Observation 的动态循环。
 */
@Component
@Slf4j
public class AgentLoop {
    @Resource
    private AgentContextBuilder contextBuilder;
    @Resource
    private AgentDecisionModel decisionModel;
    @Resource
    private ToolExecutor toolExecutor;
    @Resource
    private TurnItemService items;
    @Resource
    private TurnManager turns;
    @Resource
    private AgentRuntimeProperties properties;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private AgentCompletionService completion;

    /**
     * 执行一个 Turn，直至完成、等待、取消或达到限制。
     *
     * @param thread 长期 Thread
     * @param turn 当前 Turn
     * @param submission 启动本次 Turn 的 Submission
     * @param cancelled 协作式取消标记
     * @return 最终 Turn 状态
     */
    public AgentTurn execute(AgentThread thread, AgentTurn turn, Submission submission, AtomicBoolean cancelled) {
        Map<String, Integer> repeatedCalls = new LinkedHashMap<>();
        int failures = restoreToolHistory(turn, repeatedCalls);
        while (true) {
            if (cancelled.get()) {
                turn.cancel();
                turns.save(turn);
                return turn;
            }
            if (limitReached(turn)) {
                failByLimit(turn);
                return turn;
            }
            if (failures >= properties.getMaxToolFailures()) {
                turn.fail("TOOL_FAILURE_LIMIT_REACHED: " + properties.getMaxToolFailures());
                items.append(turn.getId(), TurnItemType.ERROR, turn.getFailureMessage(), null, null, null);
                turns.save(turn);
                return turn;
            }
            turn.recordStep();
            turn.recordLlmCall();
            turns.save(turn);
            if (turn.getStatus() == com.selfinvestor.earnings.agent.runtime.turn.TurnStatus.CANCELLED) {
                return turn;
            }
            int inputSequence = items.latestInputSequence(turn.getId());
            AgentDecision decision = decisionModel.decide(contextBuilder.build(thread, turn));
            if (cancelled.get()) {
                turn.cancel();
                turns.save(turn);
                return turn;
            }
            validate(decision);
            items.append(turn.getId(), TurnItemType.MODEL_DECISION, decision.getReasoningSummary(),
                    decision.getToolName(), null, json(decision));
            log.info("[AgentTurn] thread={} turn={} submission={} step={} llmCallCount={} decision={} tool={} status={}",
                    thread.getId(), turn.getId(), submission.getSubmissionId(), turn.getStepCount(),
                    turn.getLlmCallCount(), decision.getType(), decision.getToolName(), turn.getStatus());
            if (decision.getType() == DecisionType.TOOL_CALL) {
                if (!executeTool(thread, turn, submission, decision, repeatedCalls)) {
                    failures++;
                }
                continue;
            }
            if (decision.getType() == DecisionType.WAIT_FOR_USER) {
                boolean waiting = turns.locked(thread.getId(), () -> {
                    if (items.latestInputSequence(turn.getId()) > inputSequence) {
                        return false;
                    }
                    turn.waitForInput();
                    turns.save(turn);
                    return true;
                });
                if (waiting) {
                    return turn;
                }
                continue;
            }
            if (completion.finish(thread, turn, decision, inputSequence)) {
                return turn;
            }
        }
    }

    /**
     * 执行一次 Tool Call 并保存 Call 与 Result。
     *
     * @param thread 当前 Thread
     * @param turn 当前 Turn
     * @param submission 启动 Submission
     * @param decision Tool 决策
     * @param repeatedCalls 重复调用计数
     */
    private boolean executeTool(AgentThread thread, AgentTurn turn, Submission submission, AgentDecision decision,
                             Map<String, Integer> repeatedCalls) {
        String callId = UUID.randomUUID().toString();
        String argumentsJson = json(decision.getArguments());
        items.append(turn.getId(), TurnItemType.TOOL_CALL, decision.getReasoningSummary(),
                decision.getToolName(), callId, argumentsJson);
        String callKey = decision.getToolName() + ":" + json(new TreeMap<>(decision.getArguments()));
        int repeated = repeatedCalls.merge(callKey, 1, Integer::sum);
        ToolResult result;
        if (repeated > properties.getMaxRepeatedToolCalls()) {
            result = ToolResult.failure("Repeated tool call limit reached for " + decision.getToolName(), 0);
        } else {
            AgentToolContext context = new AgentToolContext(thread, turn, submission);
            result = toolExecutor.execute(context, decision.getToolName(), decision.getArguments());
        }
        items.append(turn.getId(), TurnItemType.TOOL_RESULT,
                result.isSuccess() ? "Tool completed" : result.getError(),
                decision.getToolName(), callId, json(result));
        log.info("[AgentToolResult] thread={} turn={} submission={} step={} tool={} arguments={} duration={} success={}",
                thread.getId(), turn.getId(), submission.getSubmissionId(), turn.getStepCount(),
                decision.getToolName(), argumentsJson, result.getDurationMillis(), result.isSuccess());
        return result.isSuccess();
    }

    /**
     * 恢复等待前的重复调用与失败计数，避免恢复 Turn 绕过限制。
     */
    private int restoreToolHistory(AgentTurn turn, Map<String, Integer> repeatedCalls) {
        int failures = 0;
        for (var item : items.history(turn.getId())) {
            try {
                if (item.getType() == TurnItemType.TOOL_CALL) {
                    Map<String, Object> arguments = mapper.readValue(item.getPayloadJson(), new TypeReference<>() { });
                    repeatedCalls.merge(item.getToolName() + ":" + json(new TreeMap<>(arguments)), 1, Integer::sum);
                } else if (item.getType() == TurnItemType.TOOL_RESULT
                        && !mapper.readTree(item.getPayloadJson()).path("success").asBoolean()) {
                    failures++;
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Cannot restore tool history", exception);
            }
        }
        return failures;
    }

    /**
     * 判断 Turn 是否已耗尽任一硬预算。
     *
     * @param turn 当前 Turn
     * @return 是否达到限制
     */
    private boolean limitReached(AgentTurn turn) {
        return turn.getStepCount() >= turn.getMaxSteps() || turn.getLlmCallCount() >= turn.getMaxLlmCalls();
    }

    /**
     * 记录限制错误并终止 Turn。
     *
     * @param turn 当前 Turn
     */
    private void failByLimit(AgentTurn turn) {
        String message = "LIMIT_REACHED: maxSteps=" + turn.getMaxSteps()
                + ", maxLlmCalls=" + turn.getMaxLlmCalls();
        turn.fail(message);
        items.append(turn.getId(), TurnItemType.ERROR, message, null, null, null);
        turns.save(turn);
    }

    /**
     * 校验模型决策是否包含当前类型的必需字段。
     *
     * @param decision 模型决策
     */
    private void validate(AgentDecision decision) {
        if (decision == null || decision.getType() == null) {
            throw new IllegalArgumentException("Agent model returned no decision type");
        }
        if (decision.getType() == DecisionType.TOOL_CALL
                && (decision.getToolName() == null || decision.getToolName().isBlank())) {
            throw new IllegalArgumentException("TOOL_CALL requires toolName");
        }
        if (decision.getType() == DecisionType.FINAL_ANSWER
                && (decision.getFinalAnswer() == null || decision.getFinalAnswer().isBlank())) {
            throw new IllegalArgumentException("FINAL_ANSWER requires finalAnswer");
        }
        if (decision.getArguments() == null) {
            decision.setArguments(new LinkedHashMap<>());
        }
    }

    /**
     * 将运行对象序列化为 Turn Item 负载。
     *
     * @param value 运行对象
     * @return JSON 文本
     */
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize agent runtime item", exception);
        }
    }
}

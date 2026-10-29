package com.selfinvestor.earnings.agent.earnings.node;

import com.selfinvestor.earnings.agent.earnings.AgentBudgetExceededException;
import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsAgentNode;
import com.selfinvestor.earnings.agent.earnings.model.*;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

/**
 * 串行执行待处理研究任务并把 Evidence、Hypothesis 和 OpenQuestion 合并到 State。
 */
@Component
@Slf4j
public class ResearchNode implements EarningsAgentNode {
    @Resource
    private EarningsAgentModel model;

    /**
     * 返回 Research 节点名称。
     *
     * @return Research 节点名称
     */
    @Override
    public AgentNodeName name() {
        return AgentNodeName.RESEARCH;
    }

    /**
     * 执行所有预算允许的待处理任务；单个任务失败不会终止整个 Graph。
     *
     * @param state 当前 Agent State
     */
    @Override
    public void execute(EarningsAgentState state) {
        for (ResearchTask task : new ArrayList<>(state.getResearchTasks())) {
            if (task.getStatus() != TaskStatus.PENDING) {
                continue;
            }
            // 为 Critic 和 Finalizer 各保留一次调用；剩余任务交由 Critic 判断。
            if (!state.hasLlmCapacity(3)) {
                break;
            }
            task.setStatus(TaskStatus.IN_PROGRESS);
            try {
                ResearchOutput output = model.research(state, task);
                mergeOutput(state, task, output);
                task.setStatus(TaskStatus.COMPLETE);
                log.info("[{}] Completed task {} category={}", state.getRunId(), task.getTaskId(), task.getCategory());
            } catch (AgentBudgetExceededException exception) {
                task.setStatus(TaskStatus.PENDING);
                state.forceBestEffort();
                break;
            } catch (RuntimeException exception) {
                task.setStatus(TaskStatus.FAILED);
                state.getOpenQuestions().add(new OpenQuestion(task.getCategory(), task.getQuestion(),
                        "Research task failed: " + safeMessage(exception)));
                log.warn("[{}] Research task failed: taskId={} category={} reason={}", state.getRunId(),
                        task.getTaskId(), task.getCategory(), safeMessage(exception));
            }
        }
    }

    /**
     * 校验并合并单个任务的结构化研究输出。
     *
     * @param state 当前 Agent State
     * @param task 当前研究任务
     * @param output 模型研究输出
     */
    private void mergeOutput(EarningsAgentState state, ResearchTask task, ResearchOutput output) {
        if (output == null) {
            throw new IllegalArgumentException("Research returned no output");
        }
        if (output.getEvidence() != null) {
            output.getEvidence().stream().filter(item -> item != null && item.getClaim() != null && !item.getClaim().isBlank())
                    .forEach(item -> {
                        if (item.getEvidenceId() == null || item.getEvidenceId().isBlank()) {
                            item.setEvidenceId(UUID.randomUUID().toString());
                        }
                        item.setTaskId(task.getTaskId());
                        if (item.getSourceType() == null) {
                            item.setSourceType(EvidenceSourceType.OTHER);
                        }
                        item.setConfidence(PlannerNode.clamp(item.getConfidence()));
                        state.getEvidence().add(item);
                    });
        }
        if (output.getHypotheses() != null) {
            output.getHypotheses().stream().filter(item -> item != null && item.getStatement() != null)
                    .forEach(item -> {
                        if (item.getHypothesisId() == null || item.getHypothesisId().isBlank()) {
                            item.setHypothesisId(UUID.randomUUID().toString());
                        }
                        state.getHypotheses().add(item);
                    });
        }
        if (output.getOpenQuestions() != null) {
            output.getOpenQuestions().stream().filter(item -> item != null && item.getQuestion() != null)
                    .forEach(state.getOpenQuestions()::add);
        }
    }

    /**
     * 生成适合写入 OpenQuestion 和日志的异常摘要。
     *
     * @param error 研究异常
     * @return 非空异常摘要
     */
    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}

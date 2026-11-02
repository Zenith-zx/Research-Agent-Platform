package com.selfinvestor.earnings.agent.earnings.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.model.*;
import com.selfinvestor.earnings.llm.LlmClient;
import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将各节点的强类型输入输出映射到共享模型策略客户端，保留原有预算和重试逻辑。
 */
@Component
@Slf4j
public class DefaultEarningsAgentModel implements EarningsAgentModel {
    @Resource
    private LlmClient client;

    @Resource
    private EarningsPromptService prompts;

    @Resource
    private ObjectMapper mapper;

    @Resource
    private EarningsProperties properties;

    /**
     * 组装 Planner 输入并请求结构化规划输出。
     *
     * @param state 当前 Agent State
     * @return Planner 输出
     */
    @Override
    public PlannerOutput plan(EarningsAgentState state) {
        Map<String, Object> input = baseInput(state);
        input.put("filingContent", state.getEvent().getSourceContent());
        return invoke(state, AgentNodeName.PLANNER, input, PlannerOutput.class);
    }

    /**
     * 组装单任务研究上下文并请求结构化 Evidence。
     *
     * @param state 当前 Agent State
     * @param task 当前研究任务
     * @return Research 输出
     */
    @Override
    public ResearchOutput research(EarningsAgentState state, ResearchTask task) {
        Map<String, Object> input = baseInput(state);
        input.put("currentTask", task);
        input.put("existingEvidence", state.getEvidence());
        input.put("filingContent", state.getEvent().getSourceContent());
        return invoke(state, AgentNodeName.RESEARCH, input, ResearchOutput.class);
    }

    /**
     * 组装研究状态并请求 Critic 充分性判断。
     *
     * @param state 当前 Agent State
     * @return Critic 结果
     */
    @Override
    public CriticResult criticize(EarningsAgentState state) {
        Map<String, Object> input = baseInput(state);
        input.put("researchPlan", state.getResearchPlan());
        input.put("researchTasks", state.getResearchTasks());
        input.put("evidence", state.getEvidence());
        input.put("hypotheses", state.getHypotheses());
        input.put("openQuestions", state.getOpenQuestions());
        return invoke(state, AgentNodeName.CRITIC, input, CriticResult.class);
    }

    /**
     * 组装已完成任务、证据和剩余预算并请求增量计划。
     *
     * @param state 当前 Agent State
     * @return Replanner 输出
     */
    @Override
    public ReplannerOutput replan(EarningsAgentState state) {
        Map<String, Object> input = baseInput(state);
        input.put("originalResearchPlan", state.getResearchPlan());
        input.put("allTasks", state.getResearchTasks());
        input.put("evidence", state.getEvidence());
        input.put("openQuestions", state.getOpenQuestions());
        input.put("criticResult", state.getCriticResult());
        input.put("remainingLlmCalls", state.getMaxLlmCalls() - state.getLlmCallCount());
        return invoke(state, AgentNodeName.REPLANNER, input, ReplannerOutput.class);
    }

    /**
     * 组装已有研究结果并请求最终结构化结论。
     *
     * @param state 当前 Agent State
     * @return 最终财报结论
     */
    @Override
    public EarningsConclusion finalizeConclusion(EarningsAgentState state) {
        Map<String, Object> input = baseInput(state);
        input.put("researchPlan", state.getResearchPlan());
        input.put("researchTasks", state.getResearchTasks());
        input.put("evidence", state.getEvidence());
        input.put("hypotheses", state.getHypotheses());
        input.put("openQuestions", state.getOpenQuestions());
        input.put("criticResult", state.getCriticResult());
        return invoke(state, AgentNodeName.FINALIZER, input, EarningsConclusion.class);
    }

    /**
     * 执行一次带有限 JSON 解析重试的 Structured Output 请求。
     *
     * @param state 当前 Agent State
     * @param node 当前节点
     * @param input 节点输入对象
     * @param resultType 结构化结果类型
     * @param <T> 结果泛型
     * @return 反序列化后的结构化结果
     */
    private <T> T invoke(EarningsAgentState state, AgentNodeName node, Object input, Class<T> resultType) {
        int retries = properties.getEarningsAgent().getJsonRetryAttempts();
        for (int attempt = 0; ; attempt++) {
            state.recordLlmCall();
            try {
                return client.generateStructured(toJson(input), prompts.prompt(node), prompts.schema(node),
                        "earnings_" + node.name().toLowerCase(), resultType);
            } catch (IllegalArgumentException exception) {
                if (attempt >= retries || !state.canCallLlm()) {
                    throw exception;
                }
                log.warn("[{}] Invalid structured output, retrying node={} attempt={}", state.getRunId(), node, attempt + 1);
            }
        }
    }

    /**
     * 创建所有节点共享的、排除大段源文本的基础输入。
     *
     * @param state 当前 Agent State
     * @return 基础输入字段
     */
    private Map<String, Object> baseInput(EarningsAgentState state) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("runId", state.getRunId());
        input.put("ticker", state.getTicker());
        input.put("companyName", state.getCompanyName());
        input.put("fiscalQuarter", state.getFiscalQuarter());
        Map<String, Object> filing = new LinkedHashMap<>();
        filing.put("filingId", state.getEvent().getFilingId());
        filing.put("accessionNumber", state.getEvent().getAccessionNumber());
        filing.put("formType", state.getEvent().getFormType());
        filing.put("filingDate", state.getEvent().getFilingDate() == null
                ? null : state.getEvent().getFilingDate().toString());
        filing.put("secUrl", state.getEvent().getSecUrl());
        input.put("filing", filing);
        input.put("currentRound", state.getCurrentRound());
        return input;
    }

    /**
     * 将节点输入序列化为发送给模型的 JSON。
     *
     * @param value 节点输入对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize earnings agent input", exception);
        }
    }
}

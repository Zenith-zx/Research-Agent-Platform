package com.selfinvestor.earnings.agent.earnings.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.earnings.EarningsAgentRunPO;
import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.repository.EarningsAgentRunRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 使用 MyBatis 和 JSON snapshot 持久化 Agent checkpoint。
 */
@Component
public class MyBatisAgentCheckpointStore implements AgentCheckpointStore {
    @Resource
    private EarningsAgentRunRepository runs;

    @Resource
    private ObjectMapper mapper;

    /**
     * 创建初始 Agent Run 记录。
     *
     * @param state 初始 Agent State
     */
    @Override
    public void create(EarningsAgentState state) {
        runs.insert(toPO(state));
    }

    /**
     * 更新 Agent Run 的最新 State snapshot。
     *
     * @param state 最新 Agent State
     */
    @Override
    public void save(EarningsAgentState state) {
        runs.update(toPO(state));
    }

    /**
     * 将业务 State 转换为 MyBatis 持久化实体并完成 JSON 序列化。
     *
     * @param state Agent State
     * @return Agent Run 持久化实体
     */
    private EarningsAgentRunPO toPO(EarningsAgentState state) {
        EarningsAgentRunPO run = new EarningsAgentRunPO();
        run.setRunId(state.getRunId());
        run.setFilingId(state.getEvent().getFilingId());
        run.setTicker(state.getTicker());
        run.setFiscalQuarter(state.getFiscalQuarter());
        run.setStatus(state.getStatus());
        run.setCurrentRound(state.getCurrentRound());
        run.setStepCount(state.getStepCount());
        run.setLlmCallCount(state.getLlmCallCount());
        run.setConfidence(state.getConfidence());
        run.setStartTime(state.getStartedAt());
        run.setEndTime(state.getEndedAt());
        run.setErrorMessage(state.getErrorMessage());
        try {
            run.setStateJson(mapper.writeValueAsString(state));
            if (state.getConclusion() != null) {
                run.setFinalResult(mapper.writeValueAsString(state.getConclusion()));
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize earnings agent checkpoint", exception);
        }
        return run;
    }
}

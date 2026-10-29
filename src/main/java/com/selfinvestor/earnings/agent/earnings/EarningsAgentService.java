package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.graph.AgentCheckpointStore;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsGraph;
import com.selfinvestor.earnings.agent.earnings.model.EarningsConclusion;
import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Earnings LongGraph 的统一入口。
 */
@Service
@Slf4j
public class EarningsAgentService {
    @Resource
    private EarningsGraph graph;

    @Resource
    private AgentCheckpointStore checkpoints;

    @Resource
    private EarningsProperties properties;

    /**
     * 初始化 State、持久化初始 checkpoint 并执行 Earnings Graph。
     *
     * @param event 可持久化的财报事件
     * @return Graph 生成的结构化财报结论
     */
    public EarningsConclusion analyze(EarningsEvent event) {
        EarningsProperties.EarningsAgent budget = properties.getEarningsAgent();
        validateBudget(budget);
        EarningsAgentState state = EarningsAgentState.initialize(event.getTicker(), event.getCompanyName(), event,
                budget.getMaxRounds(), budget.getMaxSteps(), budget.getMaxLlmCalls());
        checkpoints.create(state);
        log.info("[{}] Agent started ticker={} filingId={} maxRounds={} maxSteps={} maxLlmCalls={}",
                state.getRunId(), state.getTicker(), event.getFilingId(), state.getMaxRounds(),
                state.getMaxSteps(), state.getMaxLlmCalls());
        EarningsAgentState completed = graph.execute(state);
        if (completed.getConclusion() == null) {
            throw new IllegalStateException("Earnings agent finished without a conclusion");
        }
        return completed.getConclusion();
    }

    /**
     * 校验所有 Agent 硬预算和 JSON 重试配置。
     *
     * @param budget Agent 预算配置
     */
    private static void validateBudget(EarningsProperties.EarningsAgent budget) {
        if (budget.getMaxRounds() < 1 || budget.getMaxSteps() < 1 || budget.getMaxLlmCalls() < 1) {
            throw new IllegalStateException("Earnings agent budgets must all be at least 1");
        }
        if (budget.getJsonRetryAttempts() < 0) {
            throw new IllegalStateException("Earnings agent JSON retry attempts cannot be negative");
        }
    }
}

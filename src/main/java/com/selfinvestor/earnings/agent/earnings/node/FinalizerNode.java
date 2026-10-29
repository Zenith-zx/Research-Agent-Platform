package com.selfinvestor.earnings.agent.earnings.node;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsAgentNode;
import com.selfinvestor.earnings.agent.earnings.model.*;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

/**
 * 仅基于 State 中已有证据生成最终结构化财报结论。
 */
@Component
@Slf4j
public class FinalizerNode implements EarningsAgentNode {
    @Resource
    private EarningsAgentModel model;

    /**
     * 返回 Finalizer 节点名称。
     *
     * @return Finalizer 节点名称
     */
    @Override
    public AgentNodeName name() {
        return AgentNodeName.FINALIZER;
    }

    /**
     * 生成最终结论，并写入运行状态、置信度和结束时间。
     *
     * @param state 当前 Agent State
     */
    @Override
    public void execute(EarningsAgentState state) {
        boolean bestEffort = state.getCriticResult() == null
                || state.getCriticResult().getDecision() == CriticDecision.BEST_EFFORT_FINISH
                || state.getStatus() == AgentStatus.BEST_EFFORT;
        EarningsConclusion conclusion = state.canCallLlm()
                ? model.finalizeConclusion(state)
                : fallbackConclusion(state);
        if (conclusion == null) {
            throw new IllegalStateException("Finalizer returned no conclusion");
        }
        normalize(conclusion);
        state.setConclusion(conclusion);
        state.setConfidence(conclusion.getConfidence());
        state.setStatus(bestEffort ? AgentStatus.BEST_EFFORT : AgentStatus.COMPLETED);
        state.setEndedAt(Instant.now());
        log.info("[{}] Agent finished status={} confidence={}", state.getRunId(), state.getStatus(),
                conclusion.getConfidence());
    }

    /**
     * 在没有剩余 LLM 预算时生成保守、低置信度的确定性结论。
     *
     * @param state 当前 Agent State
     * @return 尽力而为的结构化结论
     */
    private EarningsConclusion fallbackConclusion(EarningsAgentState state) {
        EarningsConclusion conclusion = new EarningsConclusion();
        conclusion.setFundamentalTrend(FundamentalTrend.UNCHANGED);
        conclusion.setGrowthTrend(GrowthTrend.UNKNOWN);
        conclusion.setMarginTrend(MarginTrend.UNKNOWN);
        conclusion.setGuidanceAssessment(GuidanceAssessment.NO_GUIDANCE);
        conclusion.setExpectationGap(ExpectationGap.UNKNOWN);
        conclusion.setEarningsQuality(EarningsQuality.UNKNOWN);
        conclusion.setConfidence(Math.min(state.getConfidence(), 0.35));
        conclusion.setPositiveChanges(new ArrayList<>());
        conclusion.setNegativeChanges(new ArrayList<>());
        conclusion.setKeyRisks(state.getCriticResult() == null || state.getCriticResult().getMissingInformation() == null
                ? new ArrayList<>() : new ArrayList<>(state.getCriticResult().getMissingInformation()));
        conclusion.setNextQuarterWatchList(state.getOpenQuestions().stream().map(OpenQuestion::getQuestion).toList());
        conclusion.setExecutiveSummary("Agent budget was exhausted; this is a low-confidence best-effort conclusion.");
        return conclusion;
    }

    /**
     * 补齐模型可能遗漏的字段并限制置信度范围。
     *
     * @param conclusion 待规范化的最终结论
     */
    private void normalize(EarningsConclusion conclusion) {
        conclusion.setConfidence(PlannerNode.clamp(conclusion.getConfidence()));
        if (conclusion.getFundamentalTrend() == null) {
            conclusion.setFundamentalTrend(FundamentalTrend.UNCHANGED);
        }
        if (conclusion.getGrowthTrend() == null) {
            conclusion.setGrowthTrend(GrowthTrend.UNKNOWN);
        }
        if (conclusion.getMarginTrend() == null) {
            conclusion.setMarginTrend(MarginTrend.UNKNOWN);
        }
        if (conclusion.getGuidanceAssessment() == null) {
            conclusion.setGuidanceAssessment(GuidanceAssessment.NO_GUIDANCE);
        }
        if (conclusion.getExpectationGap() == null) {
            conclusion.setExpectationGap(ExpectationGap.UNKNOWN);
        }
        if (conclusion.getEarningsQuality() == null) {
            conclusion.setEarningsQuality(EarningsQuality.UNKNOWN);
        }
        if (conclusion.getPositiveChanges() == null) {
            conclusion.setPositiveChanges(new ArrayList<>());
        }
        if (conclusion.getNegativeChanges() == null) {
            conclusion.setNegativeChanges(new ArrayList<>());
        }
        if (conclusion.getKeyRisks() == null) {
            conclusion.setKeyRisks(new ArrayList<>());
        }
        if (conclusion.getNextQuarterWatchList() == null) {
            conclusion.setNextQuarterWatchList(new ArrayList<>());
        }
        if (conclusion.getExecutiveSummary() == null) {
            conclusion.setExecutiveSummary("Insufficient evidence for a summary.");
        }
    }
}

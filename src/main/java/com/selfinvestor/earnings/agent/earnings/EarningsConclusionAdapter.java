package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.model.EarningsConclusion;
import com.selfinvestor.earnings.agent.earnings.model.EarningsQuality;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.enums.FundamentalTrend;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 将 Graph 结论适配到已有 AI_ANALYSIS、REST API 和通知契约。
 */
@Component
public class EarningsConclusionAdapter {
    /**
     * 将 Graph 结论转换为旧通知与查询链路使用的 DTO。
     *
     * @param event 财报事件
     * @param conclusion Graph 最终结论
     * @return 兼容旧链路的财报分析 DTO
     */
    public FinancialAnalysisDTO toLegacyDto(EarningsEvent event, EarningsConclusion conclusion) {
        return new FinancialAnalysisDTO(
                event.getCompanyName(),
                event.getTicker(),
                null,
                conclusion.getExecutiveSummary(),
                null,
                null,
                Collections.emptyList(),
                conclusion.getPositiveChanges(),
                conclusion.getNegativeChanges(),
                conclusion.getKeyRisks(),
                Collections.emptyList(),
                mapTrend(conclusion.getFundamentalTrend()),
                conclusion.getExecutiveSummary(),
                false,
                null,
                false,
                "Graph expectation gap: " + conclusion.getExpectationGap(),
                qualityScore(conclusion.getEarningsQuality()),
                conclusion.getExecutiveSummary());
    }

    /**
     * 将 Agent 的五档基本面趋势映射为旧链路的三档趋势。
     *
     * @param trend Agent 基本面趋势
     * @return 旧链路基本面趋势
     */
    private FundamentalTrend mapTrend(com.selfinvestor.earnings.agent.earnings.model.FundamentalTrend trend) {
        if (trend == null) {
            return FundamentalTrend.STABLE;
        }
        return switch (trend) {
            case STRONGLY_IMPROVED, IMPROVED -> FundamentalTrend.IMPROVING;
            case UNCHANGED -> FundamentalTrend.STABLE;
            case DETERIORATED, STRONGLY_DETERIORATED -> FundamentalTrend.DETERIORATING;
        };
    }

    /**
     * 将结构化财报质量等级转换为旧 DTO 的十分制分数。
     *
     * @param quality 财报质量等级
     * @return 兼容旧 DTO 的质量分数；未知时返回 null
     */
    private Integer qualityScore(EarningsQuality quality) {
        if (quality == null) {
            return null;
        }
        return switch (quality) {
            case HIGH -> 9;
            case MEDIUM -> 7;
            case LOW -> 4;
            case UNKNOWN -> null;
        };
    }
}

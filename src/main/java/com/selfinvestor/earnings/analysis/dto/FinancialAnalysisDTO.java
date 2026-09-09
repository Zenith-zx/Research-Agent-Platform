package com.selfinvestor.earnings.analysis.dto;

import com.selfinvestor.earnings.enums.FundamentalTrend;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 返回的结构化财报分析结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAnalysisDTO {
    /**
     * 公司名称。
     */
    private String company;
    /**
     * 股票代码。
     */
    private String ticker;
    /**
     * 财务季度。
     */
    private String fiscalQuarter;
    /**
     * 财报核心标题。
     */
    private String headline;
    /**
     * 核心财务指标。
     */
    private FinancialsDTO financials;
    /**
     * 公司业绩指引。
     */
    private GuidanceDTO guidance;
    /**
     * 分部业务表现。
     */
    private List<SegmentDTO> segments;
    /**
     * 积极因素。
     */
    private List<String> positives;
    /**
     * 消极因素。
     */
    private List<String> negatives;
    /**
     * 主要风险。
     */
    private List<String> risks;
    /**
     * 管理层重点表述。
     */
    private List<String> managementCommentary;
    /**
     * 基本面趋势。
     */
    private FundamentalTrend fundamentalTrend;
    /**
     * 基本面趋势判断原因。
     */
    private String fundamentalTrendReason;
    /**
     * 投资逻辑是否发生变化。
     */
    private boolean investmentThesisChanged;
    /**
     * 投资逻辑变化原因。
     */
    private String investmentThesisChangeReason;
    /**
     * 是否具备一致预期对比数据。
     */
    private boolean consensusComparisonAvailable;
    /**
     * 一致预期对比说明。
     */
    private String consensusComparisonNote;
    /**
     * 财报质量评分。
     */
    private Integer qualityScore;
    /**
     * 财报分析摘要。
     */
    private String summary;

}

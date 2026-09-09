package com.selfinvestor.earnings.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 核心财务指标 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialsDTO {
    /**
     * 营业收入。
     */
    private MetricDTO revenue;
    /**
     * 毛利率。
     */
    private MarginDTO grossMargin;
    /**
     * 营业利润率。
     */
    private MarginDTO operatingMargin;
    /**
     * 营业利润。
     */
    private String operatingIncome;
    /**
     * 净利润。
     */
    private String netIncome;
    /**
     * 每股收益。
     */
    private String eps;
    /**
     * 自由现金流。
     */
    private String freeCashFlow;
    /**
     * 资本支出。
     */
    private String capex;
    /**
     * 现金余额。
     */
    private String cash;
    /**
     * 债务余额。
     */
    private String debt;
}

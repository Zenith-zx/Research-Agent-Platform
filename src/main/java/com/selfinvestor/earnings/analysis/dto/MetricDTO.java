package com.selfinvestor.earnings.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 包含数值及增长率的财务指标 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
    /**
     * 指标值。
     */
    private String value;
    /**
     * 同比增长率。
     */
    private BigDecimal yoyGrowth;
    /**
     * 环比增长率。
     */
    private BigDecimal qoqGrowth;
}

package com.selfinvestor.earnings.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 利润率指标 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarginDTO {
    /**
     * 当前利润率。
     */
    private String value;
    /**
     * 上一季度利润率。
     */
    private String previousQuarter;
    /**
     * 利润率趋势。
     */
    private String trend;
}

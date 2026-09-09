package com.selfinvestor.earnings.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 业务分部表现 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDTO {
    /**
     * 分部名称。
     */
    private String name;
    /**
     * 分部收入。
     */
    private String revenue;
    /**
     * 分部收入同比增长率。
     */
    private BigDecimal yoyGrowth;
    /**
     * 分部收入环比增长率。
     */
    private BigDecimal qoqGrowth;
    /**
     * 分部业务说明。
     */
    private String commentary;
}

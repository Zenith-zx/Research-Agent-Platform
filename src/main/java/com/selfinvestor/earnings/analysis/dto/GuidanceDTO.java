package com.selfinvestor.earnings.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司业绩指引 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuidanceDTO {
    /**
     * 下一季度收入指引下限。
     */
    private String nextQuarterRevenueLow;
    /**
     * 下一季度收入指引上限。
     */
    private String nextQuarterRevenueHigh;
    /**
     * 毛利率指引。
     */
    private String grossMargin;
    /**
     * 全年业绩指引。
     */
    private String fullYearGuidance;
}

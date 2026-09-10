package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 带口径信息的市场指标 DTO。
 */
@Data
@NoArgsConstructor
public class MarketMetricDTO {
    /**
     * 指标名称。
     */
    private String name;
    /**
     * 当前值或收益率。
     */
    private BigDecimal value;
    /**
     * 相对前一期的变化。
     */
    private BigDecimal change;
    /**
     * 数值单位。
     */
    private String unit;
    /**
     * 数据时间戳。
     */
    private Instant timestamp;
    /**
     * 数据来源。
     */
    private String source;

    /**
     * 创建市场指标。
     *
     * @param name 指标名称
     * @param value 当前值
     * @param change 前一期变化
     * @param unit 数值单位
     * @param timestamp 数据时间戳
     * @param source 数据来源
     */
    public MarketMetricDTO(String name, BigDecimal value, BigDecimal change, String unit, Instant timestamp, String source) {
        this.name = name;
        this.value = value;
        this.change = change;
        this.unit = unit;
        this.timestamp = timestamp;
        this.source = source;
    }
}

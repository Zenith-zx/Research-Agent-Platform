package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 触发标的技术快照 DTO。
 */
@Data
@NoArgsConstructor
public class SymbolSnapshotDTO {
    /**
     * 标的代码。
     */
    private String symbol;
    /**
     * 最新价格。
     */
    private BigDecimal price;
    /**
     * 单日收益率百分比。
     */
    private BigDecimal dailyReturn;
    /**
     * 五日收益率百分比。
     */
    private BigDecimal fiveDayReturn;
    /**
     * 二十日收益率百分比。
     */
    private BigDecimal twentyDayReturn;
    /**
     * RSI6。
     */
    private BigDecimal rsi6;
    /**
     * RSI14。
     */
    private BigDecimal rsi14;
    /**
     * 距离 MA20 的百分比。
     */
    private BigDecimal distanceFromMa20;
    /**
     * 距离 MA50 的百分比。
     */
    private BigDecimal distanceFromMa50;
    /**
     * 距离 MA200 的百分比。
     */
    private BigDecimal distanceFromMa200;
    /**
     * 行情时间戳。
     */
    private Instant timestamp;
    /**
     * 数据来源。
     */
    private String source;
}

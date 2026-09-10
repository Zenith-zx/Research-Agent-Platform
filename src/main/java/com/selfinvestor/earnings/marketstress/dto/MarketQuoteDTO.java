package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 最新行情 DTO。
 */
@Data
@NoArgsConstructor
public class MarketQuoteDTO {
    /**
     * 标的代码。
     */
    private String symbol;
    /**
     * 最新价格。
     */
    private BigDecimal price;
    /**
     * 日涨跌幅百分比。
     */
    private BigDecimal dailyReturn;
    /**
     * 数据时间戳。
     */
    private Instant timestamp;
    /**
     * 数据来源。
     */
    private String source;
}

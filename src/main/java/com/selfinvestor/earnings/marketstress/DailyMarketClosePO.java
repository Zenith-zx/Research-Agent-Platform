package com.selfinvestor.earnings.marketstress;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 每日正式收盘价持久化对象。
 */
@Data
@NoArgsConstructor
public class DailyMarketClosePO {
    /**
     * 数据库主键。
     */
    private Long id;
    /**
     * 标的代码。
     */
    private String symbol;
    /**
     * 交易日期。
     */
    private LocalDate tradeDate;
    /**
     * 正式收盘价。
     */
    private BigDecimal closePrice;
    /**
     * 数据来源。
     */
    private String source;
    /**
     * 创建时间。
     */
    private Instant createdAt;
}

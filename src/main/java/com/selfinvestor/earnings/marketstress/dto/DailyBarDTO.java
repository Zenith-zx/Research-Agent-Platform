package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 日线行情 DTO。
 */
@Data
@NoArgsConstructor
public class DailyBarDTO {
    /**
     * 标的代码。
     */
    private String symbol;
    /**
     * 交易日期。
     */
    private LocalDate date;
    /**
     * 开盘价。
     */
    private BigDecimal open;
    /**
     * 最高价。
     */
    private BigDecimal high;
    /**
     * 最低价。
     */
    private BigDecimal low;
    /**
     * 收盘价。
     */
    private BigDecimal close;
    /**
     * 成交量。
     */
    private BigDecimal volume;
    /**
     * 数据时间戳。
     */
    private Instant timestamp;
    /**
     * 数据来源。
     */
    private String source;

    /**
     * 创建日线行情。
     *
     * @param symbol 标的代码
     * @param date 交易日期
     * @param open 开盘价
     * @param high 最高价
     * @param low 最低价
     * @param close 收盘价
     * @param volume 成交量
     * @param timestamp 数据时间戳
     * @param source 数据来源
     */
    public DailyBarDTO(String symbol, LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low,
                       BigDecimal close, BigDecimal volume, Instant timestamp, String source) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
        this.source = source;
    }
}

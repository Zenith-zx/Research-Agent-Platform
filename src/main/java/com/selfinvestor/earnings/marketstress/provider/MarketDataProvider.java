package com.selfinvestor.earnings.marketstress.provider;

import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.enums.MarketMetricType;

import java.util.List;

/**
 * 行情数据供应商接口。
 */
public interface MarketDataProvider {
    /**
     * 获取标的最新行情。
     *
     * @param symbol 标的代码
     * @return 最新行情
     */
    MarketQuoteDTO getQuote(String symbol);

    /**
     * 获取标的日线历史行情，按日期升序返回。
     *
     * @param symbol 标的代码
     * @param days 最大交易日数量
     * @return 日线行情
     */
    List<DailyBarDTO> getDailyBars(String symbol, int days);

    /**
     * 获取宏观或跨资产指标；供应商无法提供时允许返回值为空的 DTO。
     *
     * @param type 指标类型
     * @return 市场指标
     */
    MarketMetricDTO getMetric(MarketMetricType type);
}

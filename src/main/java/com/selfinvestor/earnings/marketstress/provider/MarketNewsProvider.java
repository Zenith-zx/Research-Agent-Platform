package com.selfinvestor.earnings.marketstress.provider;

import com.selfinvestor.earnings.marketstress.dto.MarketNewsDTO;

import java.time.Duration;
import java.util.List;

/**
 * 市场新闻供应商接口。
 */
public interface MarketNewsProvider {
    /**
     * 获取指定回溯窗口内的重要市场新闻。
     *
     * @param lookback 回溯时间
     * @param limit 最大新闻数量
     * @return 按时间倒序排列并去重的新闻
     */
    List<MarketNewsDTO> getRecentMarketNews(Duration lookback, int limit);
}

package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨资产市场快照 DTO。
 */
@Data
@NoArgsConstructor
public class CrossAssetSnapshotDTO {
    /**
     * SPY 单日收益率。
     */
    private MarketMetricDTO spyReturn;
    /**
     * IWM 单日收益率。
     */
    private MarketMetricDTO iwmReturn;
    /**
     * SOXX 单日收益率。
     */
    private MarketMetricDTO soxxReturn;
    /**
     * VIX 水平和变化。
     */
    private MarketMetricDTO vix;
    /**
     * 美国两年期国债收益率和基点变化。
     */
    private MarketMetricDTO us2y;
    /**
     * 美国十年期国债收益率和基点变化。
     */
    private MarketMetricDTO us10y;
    /**
     * 美国三十年期国债收益率和基点变化。
     */
    private MarketMetricDTO us30y;
    /**
     * 美元指数和变化。
     */
    private MarketMetricDTO dxy;
    /**
     * WTI 原油和变化。
     */
    private MarketMetricDTO wti;
    /**
     * 布伦特原油和变化。
     */
    private MarketMetricDTO brent;
    /**
     * 黄金和变化。
     */
    private MarketMetricDTO gold;
    /**
     * HYG 单日收益率。
     */
    private MarketMetricDTO hygReturn;
    /**
     * LQD 单日收益率。
     */
    private MarketMetricDTO lqdReturn;
}

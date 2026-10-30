package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 跨资产市场指标类型。
 */
@Getter
public enum MarketMetricType {
    VIX("CBOE 波动率指数"),
    US2Y("美国两年期国债收益率"),
    US5Y("美国五年期国债收益率"),
    US10Y("美国十年期国债收益率"),
    US30Y("美国三十年期国债收益率"),
    DXY("美元指数"),
    WTI("WTI 原油"),
    BRENT("布伦特原油"),
    GOLD("黄金");

    /**
     * 指标描述。
     */
    private final String desc;

    /**
     * 创建市场指标类型。
     *
     * @param desc 指标描述
     */
    MarketMetricType(String desc) {
        this.desc = desc;
    }
}

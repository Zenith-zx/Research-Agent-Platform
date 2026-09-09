package com.selfinvestor.earnings.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 基本面趋势。
 */
@Getter
@AllArgsConstructor
public enum FundamentalTrend {
    IMPROVING("基本面改善"),
    STABLE("基本面稳定"),
    DETERIORATING("基本面恶化");

    /**
     * 趋势描述。
     */
    private final String desc;
}

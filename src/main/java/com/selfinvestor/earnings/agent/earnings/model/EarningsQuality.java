package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财报利润和经营表现的质量等级。
 */
@Getter
@AllArgsConstructor
public enum EarningsQuality {
    HIGH("业绩主要由可持续的核心经营改善驱动，现金流和利润质量较高"),
    MEDIUM("业绩总体可信，但存在部分一次性因素、口径差异或证据缺口"),
    LOW("业绩主要受一次性因素、会计调整或低质量增长驱动"),
    UNKNOWN("现有证据不足，无法可靠判断财报质量");

    /**
     * 质量等级说明。
     */
    private final String desc;
}

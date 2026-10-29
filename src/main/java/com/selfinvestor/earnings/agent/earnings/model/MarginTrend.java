package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 毛利率或营业利润率的变化方向。
 */
@Getter
@AllArgsConstructor
public enum MarginTrend {
    EXPANDING("毛利率或营业利润率相较可比期间上升"),
    STABLE("毛利率或营业利润率相较可比期间基本稳定"),
    CONTRACTING("毛利率或营业利润率相较可比期间下降"),
    UNKNOWN("缺少可比利润率数据，无法判断变化方向");

    /**
     * 利润率趋势说明。
     */
    private final String desc;
}

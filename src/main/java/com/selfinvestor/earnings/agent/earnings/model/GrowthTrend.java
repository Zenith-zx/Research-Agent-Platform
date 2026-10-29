package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收入或核心业务增长速度的变化方向。
 */
@Getter
@AllArgsConstructor
public enum GrowthTrend {
    ACCELERATING("收入或核心业务增长率相较可比期间正在加速"),
    STABLE("收入或核心业务增长率相较可比期间基本稳定"),
    DECELERATING("收入或核心业务增长率相较可比期间正在放缓"),
    UNKNOWN("缺少可比数据，无法判断增长速度变化");

    /**
     * 增长趋势说明。
     */
    private final String desc;
}

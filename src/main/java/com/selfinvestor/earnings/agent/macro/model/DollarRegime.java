package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的DollarRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum DollarRegime {
    STRENGTHENING("美元趋势走强"),
    STABLE("美元趋势稳定"),
    WEAKENING("美元趋势走弱"),
    UNKNOWN("缺少美元证据");

    /** 分类的中文解释。 */
    private final String desc;
}

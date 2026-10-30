package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的InflationRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum InflationRegime {
    DISINFLATION("通胀持续回落"),
    STABLE("通胀趋势稳定"),
    STICKY("通胀粘性较强"),
    REACCELERATING("通胀重新加速"),
    UNKNOWN("证据不足，尚不能判断通胀状态");

    /** 分类的中文解释。 */
    private final String desc;
}

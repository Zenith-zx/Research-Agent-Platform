package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的LiquidityRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum LiquidityRegime {
    EASING("流动性改善"),
    STABLE("流动性稳定"),
    TIGHTENING("流动性收紧"),
    STRESSED("流动性承压"),
    UNKNOWN("缺少流动性证据");

    /** 分类的中文解释。 */
    private final String desc;
}

package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的RatesRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum RatesRegime {
    FALLING("国债收益率趋势下降"),
    STABLE("国债收益率趋势稳定"),
    RISING("国债收益率趋势上升"),
    HIGH_AND_STICKY("利率维持高位且回落缓慢"),
    UNKNOWN("缺少利率证据");

    /** 分类的中文解释。 */
    private final String desc;
}

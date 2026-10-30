package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的LaborRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum LaborRegime {
    TIGHT("劳动力市场紧张"),
    BALANCED("劳动力供需较均衡"),
    COOLING("劳动力市场降温"),
    WEAK("就业市场明显疲弱"),
    UNKNOWN("证据不足，尚不能判断就业状态");

    /** 分类的中文解释。 */
    private final String desc;
}

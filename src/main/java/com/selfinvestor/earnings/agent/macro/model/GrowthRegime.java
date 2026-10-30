package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的GrowthRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum GrowthRegime {
    ACCELERATING("经济增长加速"),
    STABLE("经济增长稳定"),
    SLOWING("经济增长放缓"),
    CONTRACTION("经济活动收缩"),
    UNKNOWN("证据不足，尚不能判断增长状态");

    /** 分类的中文解释。 */
    private final String desc;
}

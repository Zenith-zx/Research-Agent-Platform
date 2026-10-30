package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的FedRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum FedRegime {
    DOVISH("美联储立场偏宽松"),
    NEUTRAL_DOVISH("中性偏鸽派"),
    NEUTRAL("政策立场中性"),
    NEUTRAL_HAWKISH("中性偏鹰派"),
    HAWKISH("美联储立场偏紧缩"),
    UNKNOWN("证据不足，尚不能判断政策立场");

    /** 分类的中文解释。 */
    private final String desc;
}

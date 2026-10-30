package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的RiskRegime分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum RiskRegime {
    RISK_ON("风险偏好上升"),
    NEUTRAL("风险偏好中性"),
    RISK_OFF("风险规避上升"),
    UNKNOWN("缺少风险偏好证据");

    /** 分类的中文解释。 */
    private final String desc;
}

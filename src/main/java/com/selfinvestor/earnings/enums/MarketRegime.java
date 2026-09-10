package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场环境分类。
 */
@Getter
public enum MarketRegime {
    RISK_ON("风险偏好"), RISK_OFF("风险规避"), VOLATILE("高波动"), CRISIS("危机状态"), TRANSITION("环境切换"), UNKNOWN("信息不足");

    /**
     * 环境描述。
     */
    private final String desc;

    /**
     * 创建市场环境。
     *
     * @param desc 环境描述
     */
    MarketRegime(String desc) {
        this.desc = desc;
    }
}

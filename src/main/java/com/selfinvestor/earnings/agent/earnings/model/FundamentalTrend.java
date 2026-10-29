package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公司核心基本面相较此前期间的变化程度。
 */
@Getter
@AllArgsConstructor
public enum FundamentalTrend {
    STRONGLY_IMPROVED("增长、利润率、现金流或指引等多个核心维度出现显著改善"),
    IMPROVED("至少一个重要经营维度改善，且没有被其他重大恶化完全抵消"),
    UNCHANGED("核心经营趋势总体稳定，改善与恶化因素大致平衡"),
    DETERIORATED("至少一个重要经营维度恶化，并对整体基本面构成实质影响"),
    STRONGLY_DETERIORATED("增长、利润率、现金流或指引等多个核心维度显著恶化");

    /**
     * 趋势说明。
     */
    private final String desc;
}

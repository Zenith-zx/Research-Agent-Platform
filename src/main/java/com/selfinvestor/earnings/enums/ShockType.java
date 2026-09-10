package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场冲击类型。
 */
@Getter
public enum ShockType {
    LIQUIDITY_SHOCK("流动性冲击"), POLICY_SHOCK("政策冲击"), GEOPOLITICAL_SHOCK("地缘政治冲击"),
    GROWTH_SHOCK("增长冲击"), INFLATION_SHOCK("通胀冲击"), CREDIT_SHOCK("信用冲击"),
    EARNINGS_SHOCK("盈利冲击"), VALUATION_CORRECTION("估值修正"),
    SYSTEMIC_FINANCIAL_STRESS("系统性金融压力"), UNKNOWN("信息不足");

    /**
     * 类型描述。
     */
    private final String desc;

    /**
     * 创建冲击类型。
     *
     * @param desc 类型描述
     */
    ShockType(String desc) {
        this.desc = desc;
    }
}

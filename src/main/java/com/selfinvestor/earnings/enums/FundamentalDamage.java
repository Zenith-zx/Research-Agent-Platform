package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 基本面损害等级。
 */
@Getter
public enum FundamentalDamage {
    NONE("未发现基本面损害"), LIMITED("损害有限"), MODERATE("中等基本面损害"), SEVERE("严重基本面损害"), UNKNOWN("信息不足");

    /**
     * 损害描述。
     */
    private final String desc;

    /**
     * 创建基本面损害等级。
     *
     * @param desc 损害描述
     */
    FundamentalDamage(String desc) {
        this.desc = desc;
    }
}

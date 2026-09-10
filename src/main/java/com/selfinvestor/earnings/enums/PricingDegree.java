package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 坏消息定价程度。
 */
@Getter
public enum PricingDegree {
    NOT_PRICED("尚未定价"), PARTIALLY_PRICED("部分定价"), MOSTLY_PRICED("大部分已定价"),
    HIGH_DEGREE_OF_PRICING("较高程度定价"), UNKNOWN("信息不足");

    /**
     * 定价描述。
     */
    private final String desc;

    /**
     * 创建定价程度。
     *
     * @param desc 定价描述
     */
    PricingDegree(String desc) {
        this.desc = desc;
    }
}

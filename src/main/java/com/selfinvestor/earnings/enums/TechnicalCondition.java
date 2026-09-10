package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 技术面状态。
 */
@Getter
public enum TechnicalCondition {
    NORMAL("技术面正常"), OVERSOLD("技术面超卖"), EXTREME_OVERSOLD("技术面极端超卖"), UNKNOWN("信息不足");

    /**
     * 状态描述。
     */
    private final String desc;

    /**
     * 创建技术面状态。
     *
     * @param desc 状态描述
     */
    TechnicalCondition(String desc) {
        this.desc = desc;
    }
}

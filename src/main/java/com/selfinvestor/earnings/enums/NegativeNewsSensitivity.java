package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场对新增坏消息的敏感度变化。
 */
@Getter
public enum NegativeNewsSensitivity {
    INCREASING("坏消息敏感度上升"), STABLE("坏消息敏感度稳定"), DECLINING("坏消息敏感度下降"), UNKNOWN("信息不足");

    /**
     * 敏感度描述。
     */
    private final String desc;

    /**
     * 创建坏消息敏感度。
     *
     * @param desc 敏感度描述
     */
    NegativeNewsSensitivity(String desc) {
        this.desc = desc;
    }
}

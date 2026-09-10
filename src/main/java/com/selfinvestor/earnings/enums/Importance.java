package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 驱动因素重要程度。
 */
@Getter
public enum Importance {
    LOW("低"), MEDIUM("中"), HIGH("高");

    /**
     * 重要程度描述。
     */
    private final String desc;

    /**
     * 创建重要程度。
     *
     * @param desc 重要程度描述
     */
    Importance(String desc) {
        this.desc = desc;
    }
}

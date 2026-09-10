package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 事件可逆性。
 */
@Getter
public enum Reversibility {
    HIGH("具有明确且较短的结束机制"), MEDIUM("部分可逆但仍有不确定性"), LOW("结构性或难以快速逆转"), UNKNOWN("信息不足");

    /**
     * 可逆性描述。
     */
    private final String desc;

    /**
     * 创建事件可逆性。
     *
     * @param desc 可逆性描述
     */
    Reversibility(String desc) {
        this.desc = desc;
    }
}

package com.selfinvestor.earnings.agent.macro.model;

import lombok.Data;

/**
 * 模型提出的单维度变更；Java 校验 from、to 与证据理由后才能落库。
 *
 * @param <T> 该维度的枚举类型
 */
@Data
public class RegimeChange<T extends Enum<T>> {
    private T from;
    private T to;
    private String reason;
}

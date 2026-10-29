package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实际业绩或指引相对于可验证市场预期的差异方向。
 */
@Getter
@AllArgsConstructor
public enum ExpectationGap {
    POSITIVE("实际业绩或指引明确高于财报发布前的可验证市场预期"),
    NEUTRAL("实际业绩或指引与财报发布前的可验证市场预期基本一致"),
    NEGATIVE("实际业绩或指引明确低于财报发布前的可验证市场预期"),
    UNKNOWN("缺少可靠的财报发布前预期数据，无法判断预期差");

    /**
     * 预期差说明。
     */
    private final String desc;
}

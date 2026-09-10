package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场新闻类别。
 */
@Getter
public enum NewsCategory {
    FEDERAL_RESERVE("美联储"),
    US_TREASURY("美国财政部与国债"),
    WHITE_HOUSE("白宫"),
    TARIFFS("关税"),
    INFLATION("通胀"),
    EMPLOYMENT("就业"),
    RECESSION("衰退"),
    MIDDLE_EAST("中东"),
    OIL("原油"),
    GEOPOLITICAL_CONFLICT("地缘政治冲突"),
    BANKING_CREDIT("银行与信用"),
    AI_SEMICONDUCTOR("人工智能与半导体"),
    MAJOR_EARNINGS("重要财报"),
    FISCAL_POLICY("财政政策"),
    GOVERNMENT_SHUTDOWN("政府停摆"),
    OTHER("其他市场新闻");

    /**
     * 类别描述。
     */
    private final String desc;

    /**
     * 创建新闻类别。
     *
     * @param desc 类别描述
     */
    NewsCategory(String desc) {
        this.desc = desc;
    }
}

package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场研究行动分类。
 */
@Getter
public enum MarketAction {
    STRONG_MEAN_REVERSION_SETUP("强均值回归研究机会"), SCALE_IN("可考虑分批建立研究仓位"),
    WAIT_FOR_CONFIRMATION("等待确认信号"), HIGH_RISK("风险较高"), AVOID_BUYING_THE_DIP("避免逆势抄底");

    /**
     * 行动描述。
     */
    private final String desc;

    /**
     * 创建市场研究行动。
     *
     * @param desc 行动描述
     */
    MarketAction(String desc) {
        this.desc = desc;
    }
}

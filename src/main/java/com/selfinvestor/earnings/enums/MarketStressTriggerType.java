package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场压力任务触发类型。
 */
@Getter
public enum MarketStressTriggerType {
    RSI_OVERSOLD("RSI 低于配置阈值"), MAJOR_NEWS("重大新闻重新触发预留类型");

    /**
     * 触发类型描述。
     */
    private final String desc;

    /**
     * 创建触发类型。
     *
     * @param desc 触发类型描述
     */
    MarketStressTriggerType(String desc) {
        this.desc = desc;
    }
}

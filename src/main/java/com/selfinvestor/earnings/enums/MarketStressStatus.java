package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场压力分析任务状态。
 */
@Getter
public enum MarketStressStatus {
    DETECTED("已检测到超卖"), CONTEXT_READY("市场上下文已准备"), ANALYZING("AI 分析中"),
    COMPLETED("分析完成"), FAILED("分析失败");

    /**
     * 状态描述。
     */
    private final String desc;

    /**
     * 创建任务状态。
     *
     * @param desc 状态描述
     */
    MarketStressStatus(String desc) {
        this.desc = desc;
    }
}

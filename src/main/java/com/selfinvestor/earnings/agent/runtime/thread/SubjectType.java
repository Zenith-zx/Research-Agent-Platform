package com.selfinvestor.earnings.agent.runtime.thread;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent Thread 对应的投资标的类型。
 */
@Getter
@AllArgsConstructor
public enum SubjectType {
    MACRO("长期宏观经济研究主题，V1 为 US_MACRO"),
    STOCK("上市公司股票"),
    ETF("交易型开放式指数基金");

    /**
     * 标的类型说明。
     */
    private final String desc;
}

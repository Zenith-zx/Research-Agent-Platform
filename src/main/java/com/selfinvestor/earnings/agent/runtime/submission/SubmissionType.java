package com.selfinvestor.earnings.agent.runtime.submission;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent Runtime 可接收的外部输入类型。
 */
@Getter
@AllArgsConstructor
public enum SubmissionType {
    CPI_RELEASED("美国消费者价格指数发布"),
    PCE_RELEASED("个人消费支出物价发布，预留触发位"),
    NFP_RELEASED("美国非农就业报告发布"),
    FOMC_RELEASED("美联储议息决议发布"),
    FED_SPEECH("美联储官员讲话，预留触发位"),
    PMI_RELEASED("采购经理指数发布，预留触发位"),
    JOBLESS_CLAIMS_RELEASED("初请失业金发布，预留触发位"),
    YIELD_MOVE("国债收益率变化，预留触发位"),
    DXY_MOVE("美元指数变化，预留触发位"),
    VIX_MOVE("波动率变化，预留触发位"),
    OIL_MOVE("原油价格变化，预留触发位"),
    MACRO_NEWS("宏观新闻，预留触发位"),
    MACRO_USER_INPUT("用户主动提交的宏观研究问题"),
    USER_INPUT("用户主动提交的研究问题"),
    EARNINGS_RELEASED("新财报发布事件"),
    PRICE_TRIGGER("价格条件触发"),
    INDICATOR_TRIGGER("技术指标条件触发"),
    STEER("向当前活跃 Turn 追加引导"),
    CANCEL("取消当前活跃 Turn");

    /**
     * 输入类型说明。
     */
    private final String desc;
}

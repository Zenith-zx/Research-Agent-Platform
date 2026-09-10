package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 市场事件阶段。
 */
@Getter
public enum EventStage {
    RISK_EMERGING("风险开始出现"), EXPECTATION_BUILDING("预期形成中"), EVENT_MATERIALIZED("事件已经发生"),
    SECOND_ORDER_EFFECTS("正在交易二阶影响"), STABILIZING("市场趋于稳定"), RESOLVING("事件逐步解决"), UNKNOWN("信息不足");

    /**
     * 阶段描述。
     */
    private final String desc;

    /**
     * 创建事件阶段。
     *
     * @param desc 阶段描述
     */
    EventStage(String desc) {
        this.desc = desc;
    }
}

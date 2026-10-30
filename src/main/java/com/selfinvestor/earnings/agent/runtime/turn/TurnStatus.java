package com.selfinvestor.earnings.agent.runtime.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent Turn 生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum TurnStatus {
    CREATED("已创建，尚未开始执行"),
    RUNNING("正在执行 Agent Loop"),
    WAITING("等待用户补充输入"),
    COMPLETED("已生成最终响应"),
    FAILED("执行失败或达到硬限制"),
    CANCELLED("已协作式取消");

    /**
     * 状态说明。
     */
    private final String desc;
}

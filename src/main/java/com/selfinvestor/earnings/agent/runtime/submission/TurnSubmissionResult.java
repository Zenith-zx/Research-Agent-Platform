package com.selfinvestor.earnings.agent.runtime.submission;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Submission 应用于 Thread 后的结果。
 */
@Getter
@AllArgsConstructor
public enum TurnSubmissionResult {
    STARTED("创建并启动了新 Turn"),
    STEERED("输入已追加到当前活跃 Turn"),
    CANCELLED("当前活跃 Turn 已取消"),
    IGNORED("当前状态下无需处理该输入");

    /**
     * 处理结果说明。
     */
    private final String desc;
}

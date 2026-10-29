package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Earnings Agent 一次运行的生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum AgentStatus {
    INITIALIZED("Agent State 已初始化，尚未开始执行节点"),
    RUNNING("Agent 正在执行 Graph 节点"),
    COMPLETED("证据充分，Agent 已正常生成最终结论"),
    BEST_EFFORT("证据或预算有限，Agent 已生成尽力而为的低置信度结论"),
    FAILED("Agent 因不可恢复错误而执行失败");

    /**
     * 状态说明。
     */
    private final String desc;
}

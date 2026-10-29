package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ResearchTask 的执行状态。
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {
    PENDING("任务等待 ResearchNode 执行"),
    IN_PROGRESS("任务正在由 ResearchNode 研究"),
    COMPLETE("任务已完成，研究结果已合并到 Agent State"),
    FAILED("任务执行失败；失败信息会作为 OpenQuestion 交给 Critic 评估");

    /**
     * 任务状态说明。
     */
    private final String desc;
}

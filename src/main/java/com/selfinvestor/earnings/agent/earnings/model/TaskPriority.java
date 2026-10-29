package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ResearchTask 的研究优先级。
 */
@Getter
@AllArgsConstructor
public enum TaskPriority {
    HIGH("直接影响核心结论，应优先研究"),
    MEDIUM("可能影响结论，但重要性低于核心财务问题"),
    LOW("用于补充背景或提高完整性，可在预算紧张时跳过");

    /**
     * 优先级说明。
     */
    private final String desc;
}

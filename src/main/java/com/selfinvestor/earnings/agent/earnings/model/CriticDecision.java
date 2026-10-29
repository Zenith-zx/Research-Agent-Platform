package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Critic 对当前证据充分性的判断，也是 Graph 条件路由的依据。
 */
@Getter
@AllArgsConstructor
public enum CriticDecision {
    SUFFICIENT("现有证据足以形成可靠结论，下一步进入 Finalizer"),
    NEED_MORE_RESEARCH("现有证据不足且新增研究可能改变结论，下一步进入 Replanner"),
    BEST_EFFORT_FINISH("继续研究价值有限或预算不足，使用现有证据尽力完成结论");

    /**
     * 决策说明。
     */
    private final String desc;
}

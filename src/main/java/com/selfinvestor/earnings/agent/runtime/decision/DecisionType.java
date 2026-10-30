package com.selfinvestor.earnings.agent.runtime.decision;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型在一次 Agent Step 中可做出的结构化决策。
 */
@Getter
@AllArgsConstructor
public enum DecisionType {
    TOOL_CALL("调用一个已注册 Tool"),
    FINAL_ANSWER("信息充分，生成最终回答"),
    WAIT_FOR_USER("缺少无法由 Tool 获得的必要信息");

    /**
     * 决策类型说明。
     */
    private final String desc;
}

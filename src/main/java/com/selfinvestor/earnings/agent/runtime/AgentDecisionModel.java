package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;

/**
 * Agent Loop 使用的结构化 LLM 决策接口。
 */
public interface AgentDecisionModel {
    /**
     * 根据当前上下文决定调用 Tool、完成回答或等待用户。
     *
     * @param context 当前 Agent 上下文
     * @return 结构化决策
     */
    AgentDecision decide(AgentContext context);
}

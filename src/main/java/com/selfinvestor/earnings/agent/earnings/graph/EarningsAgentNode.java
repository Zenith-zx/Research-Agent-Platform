package com.selfinvestor.earnings.agent.earnings.graph;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;

/**
 * Earnings Graph 节点的统一执行契约。
 */
public interface EarningsAgentNode {
    /**
     * 返回节点在 Graph 中的唯一名称。
     *
     * @return 节点名称
     */
    AgentNodeName name();

    /**
     * 读取并更新唯一的 Agent State。
     *
     * @param state 当前 Agent State
     */
    void execute(EarningsAgentState state);
}

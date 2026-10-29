package com.selfinvestor.earnings.agent.earnings.graph;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;

/**
 * Agent State checkpoint 的持久化边界。
 */
public interface AgentCheckpointStore {
    /**
     * 创建初始 Agent Run 和 State checkpoint。
     *
     * @param state 初始 Agent State
     */
    void create(EarningsAgentState state);

    /**
     * 保存节点执行后的最新 Agent State。
     *
     * @param state 最新 Agent State
     */
    void save(EarningsAgentState state);
}

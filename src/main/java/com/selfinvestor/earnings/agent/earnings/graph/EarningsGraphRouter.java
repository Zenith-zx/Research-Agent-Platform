package com.selfinvestor.earnings.agent.earnings.graph;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.model.CriticDecision;
import org.springframework.stereotype.Component;

/**
 * Graph 的条件边集中在这里，模型不能直接选择任意 Java 执行路径。
 */
@Component
public class EarningsGraphRouter {

    /**
     * 根据当前节点和 State 选择下一节点。
     *
     * @param current 当前节点
     * @param state 当前 Agent State
     * @return 下一节点
     */
    public AgentNodeName next(AgentNodeName current, EarningsAgentState state) {
        return switch (current) {
            case PLANNER -> AgentNodeName.RESEARCH;
            case RESEARCH -> AgentNodeName.CRITIC;
            case CRITIC -> routeAfterCritic(state);
            case REPLANNER -> AgentNodeName.RESEARCH;
            case FINALIZER, END -> AgentNodeName.END;
        };
    }

    /**
     * 根据 CriticDecision 和 Java 硬预算选择 Finalizer 或 Replanner。
     *
     * @param state 包含 CriticResult 和预算计数的 State
     * @return Critic 后的下一节点
     */
    public AgentNodeName routeAfterCritic(EarningsAgentState state) {
        if (state.getCriticResult() == null || state.getCriticResult().getDecision() == null) {
            throw new IllegalStateException("Critic did not provide a routing decision");
        }
        CriticDecision decision = state.getCriticResult().getDecision();
        return switch (decision) {
            case SUFFICIENT, BEST_EFFORT_FINISH -> AgentNodeName.FINALIZER;
            case NEED_MORE_RESEARCH -> {
                if (state.shouldFinishAfterCritic()) {
                    state.forceBestEffort();
                    yield AgentNodeName.FINALIZER;
                }
                yield AgentNodeName.REPLANNER;
            }
        };
    }
}

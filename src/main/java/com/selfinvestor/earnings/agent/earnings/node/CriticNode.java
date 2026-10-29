package com.selfinvestor.earnings.agent.earnings.node;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsAgentNode;
import com.selfinvestor.earnings.agent.earnings.model.CriticResult;
import com.selfinvestor.earnings.agent.earnings.model.AgentStatus;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审查证据质量、信息缺口和矛盾，并给出条件路由决策。
 */
@Component
@Slf4j
public class CriticNode implements EarningsAgentNode {
    @Resource
    private EarningsAgentModel model;

    /**
     * 返回 Critic 节点名称。
     *
     * @return Critic 节点名称
     */
    @Override
    public AgentNodeName name() {
        return AgentNodeName.CRITIC;
    }

    /**
     * 在保留 Finalizer 预算的前提下生成并规范化 CriticResult。
     *
     * @param state 当前 Agent State
     */
    @Override
    public void execute(EarningsAgentState state) {
        if (state.getStatus() == AgentStatus.BEST_EFFORT) {
            state.forceBestEffort();
            return;
        }
        // 最后一次调用留给 Finalizer，保证预算耗尽也能产生结构化结论。
        if (!state.hasLlmCapacity(2)) {
            state.forceBestEffort();
            return;
        }
        CriticResult result = model.criticize(state);
        if (result == null || result.getDecision() == null) {
            throw new IllegalStateException("Critic returned no decision");
        }
        result.setEvidenceQuality(PlannerNode.clamp(result.getEvidenceQuality()));
        result.setConfidence(PlannerNode.clamp(result.getConfidence()));
        state.setCriticResult(result);
        state.setConfidence(result.getConfidence());
        log.info("[{}] Critic decision: {} evidenceQuality={} confidence={}", state.getRunId(),
                result.getDecision(), result.getEvidenceQuality(), result.getConfidence());
    }
}

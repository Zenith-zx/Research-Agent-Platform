package com.selfinvestor.earnings.agent.earnings.graph;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.model.AgentStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量有状态 Graph 执行器。顺序由边和状态决定，不在业务服务中硬编码调用链。
 */
@Component
@Slf4j
public class EarningsGraph {
    private final Map<AgentNodeName, EarningsAgentNode> nodes = new EnumMap<>(AgentNodeName.class);

    @Resource
    private List<EarningsAgentNode> nodeList;

    @Resource
    private EarningsGraphRouter router;

    @Resource
    private AgentCheckpointStore checkpoints;

    /**
     * 按 Spring 注入的节点集合建立节点注册表。
     */
    @PostConstruct
    void initialize() {
        nodes.clear();
        for (EarningsAgentNode node : nodeList) {
            EarningsAgentNode previous = nodes.put(node.name(), node);
            if (previous != null) {
                throw new IllegalStateException("Duplicate earnings graph node: " + node.name());
            }
        }
    }

    /**
     * 从 State 指定的下一节点开始执行，直至到达 END 或失败。
     *
     * @param state 可持久化的 Agent State
     * @return 执行完成后的 Agent State
     */
    public EarningsAgentState execute(EarningsAgentState state) {
        state.setStatus(AgentStatus.RUNNING);
        AgentNodeName current = AgentNodeName.valueOf(state.getNextNode());
        try {
            while (current != AgentNodeName.END) {
                if (current != AgentNodeName.FINALIZER && state.getStepCount() >= state.getMaxSteps() - 1) {
                    state.forceBestEffort();
                    current = AgentNodeName.FINALIZER;
                }
                EarningsAgentNode node = nodes.get(current);
                if (node == null) {
                    throw new IllegalStateException("Missing earnings graph node: " + current);
                }
                state.recordStep();
                log.info("[{}] Node entered: {} step={} round={} llmCalls={}", state.getRunId(), current,
                        state.getStepCount(), state.getCurrentRound(), state.getLlmCallCount());
                node.execute(state);
                current = router.next(current, state);
                state.setNextNode(current.name());
                checkpoints.save(state);
            }
            return state;
        } catch (RuntimeException exception) {
            state.fail(exception);
            checkpoints.save(state);
            throw exception;
        }
    }
}

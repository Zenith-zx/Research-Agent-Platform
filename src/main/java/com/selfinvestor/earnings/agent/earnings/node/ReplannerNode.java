package com.selfinvestor.earnings.agent.earnings.node;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsAgentNode;
import com.selfinvestor.earnings.agent.earnings.model.ReplannerOutput;
import com.selfinvestor.earnings.agent.earnings.model.ResearchTask;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 根据 Critic 缺口创建不重复且能实质改变结论的增量研究任务。
 */
@Component
@Slf4j
public class ReplannerNode implements EarningsAgentNode {
    private static final int MAX_NEW_TASKS = 4;

    @Resource
    private EarningsAgentModel model;

    /**
     * 返回 Replanner 节点名称。
     *
     * @return Replanner 节点名称
     */
    @Override
    public AgentNodeName name() {
        return AgentNodeName.REPLANNER;
    }

    /**
     * 增加研究轮次、调用重新规划模型并去除已有任务。
     *
     * @param state 当前 Agent State
     */
    @Override
    public void execute(EarningsAgentState state) {
        if (state.getCurrentRound() >= state.getMaxRounds() || !state.hasLlmCapacity(2)) {
            state.forceBestEffort();
            return;
        }
        state.setCurrentRound(state.getCurrentRound() + 1);
        ReplannerOutput output = model.replan(state);
        List<ResearchTask> candidates = PlannerNode.normalizeTasks(
                output == null ? null : output.getResearchTasks(), MAX_NEW_TASKS);
        Set<String> existing = new HashSet<>();
        state.getResearchTasks().forEach(task -> existing.add(key(task)));
        int before = state.getResearchTasks().size();
        for (ResearchTask task : candidates) {
            if (existing.add(key(task))) {
                state.getResearchTasks().add(task);
            }
        }
        int created = state.getResearchTasks().size() - before;
        if (created == 0) {
            state.forceBestEffort();
        }
        log.info("[{}] Round: {} Replanner created {} tasks", state.getRunId(), state.getCurrentRound(), created);
    }

    /**
     * 生成用于跨轮次任务去重的稳定业务键。
     *
     * @param task 研究任务
     * @return 分类和规范化问题组成的去重键
     */
    private static String key(ResearchTask task) {
        String question = task.getQuestion() == null ? "" : task.getQuestion().trim().toLowerCase(Locale.ROOT);
        return task.getCategory() + "|" + question;
    }
}

package com.selfinvestor.earnings.agent.earnings.node;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.graph.EarningsAgentNode;
import com.selfinvestor.earnings.agent.earnings.model.*;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 根据财报内容创建首轮研究计划、任务和初始假设。
 */
@Component
@Slf4j
public class PlannerNode implements EarningsAgentNode {
    private static final int MAX_TASKS = 8;

    @Resource
    private EarningsAgentModel model;

    /**
     * 返回 Planner 节点名称。
     *
     * @return Planner 节点名称
     */
    @Override
    public AgentNodeName name() {
        return AgentNodeName.PLANNER;
    }

    /**
     * 调用规划模型并将规范化后的计划、任务和假设写入 State。
     *
     * @param state 当前 Agent State
     */
    @Override
    public void execute(EarningsAgentState state) {
        PlannerOutput output = model.plan(state);
        if (output == null || output.getResearchPlan() == null) {
            throw new IllegalStateException("Planner returned no research plan");
        }
        state.setFiscalQuarter(output.getFiscalQuarter());
        state.setResearchPlan(output.getResearchPlan());
        state.setResearchTasks(normalizeTasks(output.getResearchTasks(), MAX_TASKS));
        state.setHypotheses(normalizeHypotheses(output.getInitialHypotheses()));
        log.info("[{}] Planner created {} tasks", state.getRunId(), state.getResearchTasks().size());
    }

    /**
     * 过滤无效任务、补齐默认字段并执行 Java 侧任务数量上限。
     *
     * @param tasks 模型返回的任务
     * @param limit 最大任务数
     * @return 可安全执行的任务列表
     */
    static List<ResearchTask> normalizeTasks(List<ResearchTask> tasks, int limit) {
        List<ResearchTask> result = new ArrayList<>();
        if (tasks == null) {
            return result;
        }
        for (ResearchTask task : tasks) {
            if (task == null || task.getQuestion() == null || task.getQuestion().isBlank()) {
                continue;
            }
            if (task.getTaskId() == null || task.getTaskId().isBlank()) {
                task.setTaskId(UUID.randomUUID().toString());
            }
            if (task.getCategory() == null) {
                task.setCategory(ResearchCategory.OTHER);
            }
            if (task.getPriority() == null) {
                task.setPriority(TaskPriority.MEDIUM);
            }
            task.setStatus(TaskStatus.PENDING);
            result.add(task);
            if (result.size() == limit) {
                break;
            }
        }
        return result;
    }

    /**
     * 过滤无效假设并补齐标识、状态和置信度边界。
     *
     * @param hypotheses 模型返回的假设
     * @return 规范化后的假设列表
     */
    private static List<Hypothesis> normalizeHypotheses(List<Hypothesis> hypotheses) {
        List<Hypothesis> result = hypotheses == null ? new ArrayList<>() : new ArrayList<>(hypotheses);
        result.removeIf(item -> item == null || item.getStatement() == null || item.getStatement().isBlank());
        result.forEach(item -> {
            if (item.getHypothesisId() == null || item.getHypothesisId().isBlank()) {
                item.setHypothesisId(UUID.randomUUID().toString());
            }
            if (item.getStatus() == null) {
                item.setStatus(HypothesisStatus.UNTESTED);
            }
            item.setConfidence(clamp(item.getConfidence()));
        });
        return result;
    }

    /**
     * 将置信度限制在零到一之间。
     *
     * @param value 原始置信度
     * @return 限制后的置信度
     */
    static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

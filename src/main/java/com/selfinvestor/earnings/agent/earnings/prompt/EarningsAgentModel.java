package com.selfinvestor.earnings.agent.earnings.prompt;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentState;
import com.selfinvestor.earnings.agent.earnings.model.*;

/**
 * 五个 Earnings Agent 节点需要的结构化模型能力接口。
 */
public interface EarningsAgentModel {
    /**
     * 为当前财报创建首轮研究计划。
     *
     * @param state 当前 Agent State
     * @return 规划输出
     */
    PlannerOutput plan(EarningsAgentState state);

    /**
     * 针对单个 ResearchTask 提取证据和开放问题。
     *
     * @param state 当前 Agent State
     * @param task 当前研究任务
     * @return 研究输出
     */
    ResearchOutput research(EarningsAgentState state, ResearchTask task);

    /**
     * 审查当前证据是否足以形成最终结论。
     *
     * @param state 当前 Agent State
     * @return Critic 审查结果
     */
    CriticResult criticize(EarningsAgentState state);

    /**
     * 根据 Critic 缺口创建增量研究任务。
     *
     * @param state 当前 Agent State
     * @return 重新规划输出
     */
    ReplannerOutput replan(EarningsAgentState state);

    /**
     * 仅使用 State 中已有研究结果生成最终结论。
     *
     * @param state 当前 Agent State
     * @return 结构化财报结论
     */
    EarningsConclusion finalizeConclusion(EarningsAgentState state);
}

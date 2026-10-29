package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Earnings Graph 中唯一的数据传递载体，可直接序列化为 checkpoint。
 */
@Data
@NoArgsConstructor
public class EarningsAgentState implements Serializable {
    private String runId;
    private String ticker;
    private String companyName;
    private String fiscalQuarter;
    private EarningsEvent event;

    private ResearchPlan researchPlan;
    private List<ResearchTask> researchTasks = new ArrayList<>();
    private List<Evidence> evidence = new ArrayList<>();
    private List<Hypothesis> hypotheses = new ArrayList<>();
    private List<OpenQuestion> openQuestions = new ArrayList<>();
    private CriticResult criticResult;
    private EarningsConclusion conclusion;

    private int currentRound;
    private int stepCount;
    private int llmCallCount;
    private int maxRounds;
    private int maxSteps;
    private int maxLlmCalls;
    private AgentStatus status;
    private double confidence;
    private String nextNode;
    private Instant startedAt;
    private Instant endedAt;
    private String errorMessage;

    /**
     * 创建一份具有独立 runId 和硬预算的新 Agent State。
     *
     * @param ticker 股票代码
     * @param companyName 公司名称
     * @param event 财报事件
     * @param maxRounds 最大研究轮数
     * @param maxSteps 最大节点执行数
     * @param maxLlmCalls 最大 LLM 调用数
     * @return 初始化后的 Agent State
     */
    public static EarningsAgentState initialize(String ticker, String companyName, EarningsEvent event,
                                                int maxRounds, int maxSteps, int maxLlmCalls) {
        EarningsAgentState state = new EarningsAgentState();
        state.runId = UUID.randomUUID().toString();
        state.ticker = ticker;
        state.companyName = companyName;
        state.event = event;
        state.currentRound = 1;
        state.maxRounds = maxRounds;
        state.maxSteps = maxSteps;
        state.maxLlmCalls = maxLlmCalls;
        state.status = AgentStatus.INITIALIZED;
        state.startedAt = Instant.now();
        state.nextNode = "PLANNER";
        return state;
    }

    /**
     * 记录一次节点执行，并在超过最大步骤数时阻止继续执行。
     */
    public void recordStep() {
        if (stepCount >= maxSteps) {
            throw new AgentBudgetExceededException("Maximum graph steps reached");
        }
        stepCount++;
    }

    /**
     * 记录一次逻辑 LLM 调用，并在超过调用预算时阻止请求。
     */
    public void recordLlmCall() {
        if (!canCallLlm()) {
            throw new AgentBudgetExceededException("Maximum LLM calls reached");
        }
        llmCallCount++;
    }

    /**
     * 判断当前是否仍允许至少一次 LLM 调用。
     *
     * @return 是否存在 LLM 调用额度
     */
    public boolean canCallLlm() {
        return llmCallCount < maxLlmCalls;
    }

    /**
     * 判断剩余额度能否容纳指定数量的 LLM 调用。
     *
     * @param requiredCalls 所需调用次数
     * @return 剩余额度是否充足
     */
    public boolean hasLlmCapacity(int requiredCalls) {
        return maxLlmCalls - llmCallCount >= requiredCalls;
    }

    /**
     * 判断 Critic 请求继续研究时是否必须因轮数、步骤或调用预算而结束。
     *
     * @return 是否必须转入尽力完成路径
     */
    public boolean shouldFinishAfterCritic() {
        return status == AgentStatus.BEST_EFFORT || currentRound >= maxRounds
                || stepCount >= maxSteps - 1 || !hasLlmCapacity(1);
    }

    /**
     * 将当前运行和 Critic 决策切换为尽力完成状态。
     */
    public void forceBestEffort() {
        if (criticResult == null) {
            criticResult = new CriticResult();
        }
        criticResult.setDecision(CriticDecision.BEST_EFFORT_FINISH);
        status = AgentStatus.BEST_EFFORT;
    }

    /**
     * 将 Agent 标记为失败并记录可持久化的错误信息。
     *
     * @param error 导致失败的异常
     */
    public void fail(Throwable error) {
        status = AgentStatus.FAILED;
        endedAt = Instant.now();
        String message = error == null ? "Unknown error" : error.getMessage();
        errorMessage = message == null ? error.getClass().getSimpleName() : message.substring(0, Math.min(2000, message.length()));
    }
}

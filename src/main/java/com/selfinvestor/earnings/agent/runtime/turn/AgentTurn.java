package com.selfinvestor.earnings.agent.runtime.turn;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thread 中一次完整的 Agent 研究任务。
 */
@Data
@NoArgsConstructor
public class AgentTurn {
    private String id;
    private String threadId;
    private String submissionId;
    private String submissionMetadataJson;
    private TurnStatus status;
    private String triggerType;
    private String goal;
    private int stepCount;
    private int llmCallCount;
    private int maxSteps;
    private int maxLlmCalls;
    private String finalResponse;
    private String failureMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建尚未启动的 Turn。
     *
     * @param threadId 所属 Thread
     * @param triggerType 触发类型
     * @param goal 研究目标
     * @param maxSteps 最大决策步数
     * @param maxLlmCalls 最大模型调用次数
     * @return 新 Turn
     */
    public static AgentTurn create(String threadId, String triggerType, String goal,
                                   int maxSteps, int maxLlmCalls) {
        AgentTurn turn = new AgentTurn();
        LocalDateTime now = LocalDateTime.now();
        turn.setId(UUID.randomUUID().toString());
        turn.setThreadId(threadId);
        turn.setStatus(TurnStatus.CREATED);
        turn.setTriggerType(triggerType);
        turn.setGoal(goal);
        turn.setMaxSteps(maxSteps);
        turn.setMaxLlmCalls(maxLlmCalls);
        turn.setCreatedAt(now);
        turn.setUpdatedAt(now);
        return turn;
    }

    /**
     * 将 Turn 切换为运行状态。
     */
    public void start() {
        status = TurnStatus.RUNNING;
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * 记录一次 Agent 决策步骤。
     */
    public void recordStep() {
        stepCount++;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 记录一次模型调用。
     */
    public void recordLlmCall() {
        llmCallCount++;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 将 Turn 置为等待输入。
     */
    public void waitForInput() {
        status = TurnStatus.WAITING;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 保存最终响应并完成 Turn。
     *
     * @param response 最终响应
     */
    public void complete(String response) {
        status = TurnStatus.COMPLETED;
        finalResponse = response;
        completedAt = LocalDateTime.now();
        updatedAt = completedAt;
    }

    /**
     * 将 Turn 标记为失败。
     *
     * @param message 失败原因
     */
    public void fail(String message) {
        status = TurnStatus.FAILED;
        failureMessage = truncate(message);
        completedAt = LocalDateTime.now();
        updatedAt = completedAt;
    }

    /**
     * 协作式取消 Turn。
     */
    public void cancel() {
        status = TurnStatus.CANCELLED;
        failureMessage = "Turn cancelled";
        completedAt = LocalDateTime.now();
        updatedAt = completedAt;
    }

    /**
     * 将错误消息限制为数据库字段允许的长度。
     *
     * @param message 原始错误消息
     * @return 安全错误消息
     */
    private String truncate(String message) {
        String value = message == null ? "Unknown agent error" : message;
        return value.substring(0, Math.min(2000, value.length()));
    }
}

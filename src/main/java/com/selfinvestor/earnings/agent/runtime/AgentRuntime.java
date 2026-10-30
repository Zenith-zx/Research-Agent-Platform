package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemType;
import com.selfinvestor.earnings.agent.runtime.turn.TurnManager;
import com.selfinvestor.earnings.agent.runtime.turn.TurnStatus;
import com.selfinvestor.earnings.event.AgentTurnCompletedEvent;
import com.selfinvestor.earnings.event.AgentTurnFailedEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理 Agent Loop 生命周期、协作式取消和完成事件。
 */
@Component
@Slf4j
public class AgentRuntime {
    @Resource
    private AgentLoop loop;
    @Resource
    private TurnManager turns;
    @Resource
    private TurnItemService items;
    @Resource
    private ApplicationEventPublisher events;
    @Resource
    private ObjectMapper mapper;

    private final ConcurrentHashMap<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> executionLocks = new ConcurrentHashMap<>();

    /**
     * 同步执行一个 Turn；调用方负责选择执行线程。
     *
     * @param thread 当前 Thread
     * @param turn 当前 Turn
     * @param submission 启动 Submission
     */
    public void start(AgentThread thread, AgentTurn turn, Submission submission) {
        synchronized (executionLocks.computeIfAbsent(thread.getId(), ignored -> new Object())) {
            run(thread, turn, submission);
        }
    }

    /**
     * 同一 Thread 的 Loop 串行执行；取消后的旧 Loop 退出前新 Loop 不会启动。
     */
    private void run(AgentThread thread, AgentTurn turn, Submission submission) {
        AtomicBoolean token = new AtomicBoolean();
        if (cancellations.putIfAbsent(turn.getId(), token) != null) {
            log.debug("Agent turn is already running: turn={}", turn.getId());
            return;
        }
        AgentTurn current = turn;
        Submission effectiveSubmission = submission;
        try {
            current = turns.require(turn.getId());
            if (current.getStatus() == TurnStatus.CANCELLED
                    || current.getStatus() == TurnStatus.COMPLETED
                    || current.getStatus() == TurnStatus.FAILED) {
                return;
            }
            effectiveSubmission = restoreSubmission(current, submission);
            current.start();
            turns.save(current);
            AgentTurn completed = loop.execute(thread, current, effectiveSubmission, token);
            if (completed.getStatus() == TurnStatus.COMPLETED) {
                events.publishEvent(new AgentTurnCompletedEvent(thread, completed, effectiveSubmission));
            } else if (completed.getStatus() == TurnStatus.FAILED
                    || completed.getStatus() == TurnStatus.CANCELLED) {
                events.publishEvent(new AgentTurnFailedEvent(thread, completed, effectiveSubmission));
            }
        } catch (RuntimeException exception) {
            current.fail(exception.getMessage());
            items.append(current.getId(), TurnItemType.ERROR, current.getFailureMessage(), null, null, null);
            turns.save(current);
            events.publishEvent(new AgentTurnFailedEvent(thread, current, effectiveSubmission));
            log.error("Agent turn failed: thread={} turn={} submission={} error={}", thread.getId(), current.getId(),
                    submission.getSubmissionId(), exception.getMessage(), exception);
        } finally {
            cancellations.remove(turn.getId());
        }
    }

    /**
     * 请求正在运行的 Turn 在下一安全检查点取消。
     *
     * @param thread 当前 Thread
     * @param turn 已取消 Turn
     * @param submission 取消 Submission
     */
    public void cancel(AgentThread thread, AgentTurn turn, Submission submission) {
        AtomicBoolean token = cancellations.get(turn.getId());
        if (token != null) {
            token.set(true);
            return;
        }
        events.publishEvent(new AgentTurnFailedEvent(thread, turn, restoreSubmission(turn, submission)));
    }

    /**
     * 在 Steer 或 Cancel 后恢复启动 Turn 的原 Submission 类型和 metadata。
     *
     * @param turn 持久化 Turn
     * @param current 当前触发 Runtime 的 Submission
     * @return 原始 Submission 语义
     */
    private Submission restoreSubmission(AgentTurn turn, Submission current) {
        if (turn.getSubmissionId() == null || turn.getSubmissionId().equals(current.getSubmissionId())) {
            return current;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (turn.getSubmissionMetadataJson() != null && !turn.getSubmissionMetadataJson().isBlank()) {
            try {
                metadata = mapper.readValue(turn.getSubmissionMetadataJson(), new TypeReference<>() {
                });
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Cannot restore submission metadata", exception);
            }
        }
        return Submission.builder()
                .submissionId(turn.getSubmissionId())
                .threadId(turn.getThreadId())
                .type(com.selfinvestor.earnings.agent.runtime.submission.SubmissionType.valueOf(turn.getTriggerType()))
                .input(current.getInput())
                .goal(turn.getGoal())
                .metadata(metadata)
                .build();
    }
}

package com.selfinvestor.earnings.agent.runtime.turn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.submission.SubmissionType;
import com.selfinvestor.earnings.agent.runtime.submission.TurnSubmissionOutcome;
import com.selfinvestor.earnings.agent.runtime.submission.TurnSubmissionResult;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import com.selfinvestor.earnings.repository.AgentTurnRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现同一 Thread 最多一个 Active Turn 的 start-or-steer 语义。
 */
@Service
public class TurnManager {
    @Resource
    private AgentTurnRepository repository;
    @Resource
    private TurnItemService items;
    @Resource
    private AgentRuntimeProperties properties;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles profiles;

    private final ConcurrentHashMap<String, Object> threadLocks = new ConcurrentHashMap<>();

    /**
     * 没有活跃 Turn 时创建新 Turn，否则将输入追加为 Steer。
     *
     * @param thread 目标 Thread
     * @param submission 外部输入
     * @return 启动或引导结果
     */
    public TurnSubmissionOutcome startOrSteer(AgentThread thread, Submission submission) {
        synchronized (threadLocks.computeIfAbsent(thread.getId(), ignored -> new Object())) {
            Optional<AgentTurn> active = repository.findActiveByThreadId(thread.getId());
            if (submission.getType() == SubmissionType.CANCEL) {
                return cancel(active);
            }
            if (active.isPresent()) {
                AgentTurn turn = active.get();
                items.append(turn.getId(), TurnItemType.STEER_INPUT, inputText(submission),
                        null, null, json(java.util.Map.of("submissionId", submission.getSubmissionId(),
                                "type", submission.getType(), "metadata", submission.getMetadata())));
                return new TurnSubmissionOutcome(TurnSubmissionResult.STEERED, turn);
            }
            AgentTurn turn = AgentTurn.create(thread.getId(), submission.getType().name(), submission.getGoal(),
                    profiles.resolve(thread).getMaxSteps(), profiles.resolve(thread).getMaxLlmCalls());
            turn.setSubmissionId(submission.getSubmissionId());
            turn.setSubmissionMetadataJson(json(submission.getMetadata()));
            turn.start();
            repository.insert(turn);
            TurnItemType inputType = submission.getType() == SubmissionType.USER_INPUT
                    ? TurnItemType.USER_INPUT : TurnItemType.TRIGGER_INPUT;
            items.append(turn.getId(), inputType, inputText(submission), null, null, null);
            return new TurnSubmissionOutcome(TurnSubmissionResult.STARTED, turn);
        }
    }

    /**
     * 持久化 Turn 当前状态和计数。
     *
     * @param turn Turn 实体
     */
    public void save(AgentTurn turn) {
        locked(turn.getThreadId(), () -> {
            Optional<AgentTurn> stored = repository.findById(turn.getId());
            if (stored.isPresent() && stored.get().getStatus() == TurnStatus.CANCELLED) {
                turn.cancel();
                return null;
            }
            repository.update(turn);
            return null;
        });
    }

    /**
     * 共享短临界区，串行化同一 Thread 的输入、取消和最终提交，不持锁调用模型。
     */
    public <T> T locked(String threadId, java.util.function.Supplier<T> action) {
        synchronized (threadLocks.computeIfAbsent(threadId, ignored -> new Object())) {
            return action.get();
        }
    }

    /**
     * 按标识重新加载 Turn。
     *
     * @param turnId Turn 标识
     * @return Turn
     */
    public AgentTurn require(String turnId) {
        return repository.findById(turnId)
                .orElseThrow(() -> new IllegalArgumentException("Agent turn not found: " + turnId));
    }

    /**
     * 取消可选的活跃 Turn。
     *
     * @param active 活跃 Turn
     * @return 取消结果
     */
    private TurnSubmissionOutcome cancel(Optional<AgentTurn> active) {
        if (active.isEmpty()) {
            return new TurnSubmissionOutcome(TurnSubmissionResult.IGNORED, null);
        }
        AgentTurn turn = active.get();
        turn.cancel();
        repository.update(turn);
        items.append(turn.getId(), TurnItemType.ERROR, "Turn cancelled by submission",
                null, null, null);
        return new TurnSubmissionOutcome(TurnSubmissionResult.CANCELLED, turn);
    }

    /**
     * 安全读取 Submission 输入文本。
     *
     * @param submission 外部输入
     * @return 输入文本
     */
    private String inputText(Submission submission) {
        return submission.getInput() == null ? "" : submission.getInput().asText();
    }

    /**
     * 序列化启动 Submission 的 metadata，供等待后恢复原始上下文。
     *
     * @param value metadata 字段
     * @return JSON 文本
     */
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize submission metadata", exception);
        }
    }
}

package com.selfinvestor.earnings.agent.runtime.submission;

import com.selfinvestor.earnings.agent.runtime.AgentRuntime;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.ThreadManager;
import com.selfinvestor.earnings.agent.runtime.turn.TurnManager;
import com.selfinvestor.earnings.agent.runtime.turn.TurnStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 持续消费 Submission，并按 Thread 分发 start、steer 或 cancel。
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "self-investor.agent", name = "enabled", havingValue = "true")
public class SubmissionDispatcher {
    @Resource
    private SubmissionQueue queue;
    @Resource
    private ThreadManager threads;
    @Resource
    private TurnManager turns;
    @Resource
    private AgentRuntime runtime;
    @Resource(name = "agentRuntimeExecutor")
    private Executor runtimeExecutor;

    private volatile boolean running;
    private Thread dispatcherThread;

    /**
     * 启动单一队列消费循环。
     */
    @PostConstruct
    void start() {
        running = true;
        dispatcherThread = new Thread(this::dispatchLoop, "agent-submission-dispatcher");
        dispatcherThread.setDaemon(true);
        dispatcherThread.start();
    }

    /**
     * 请求队列消费循环停止。
     */
    @PreDestroy
    void stop() {
        running = false;
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
        }
    }

    /**
     * 循环读取并分发 Submission，单条失败不会终止消费者。
     */
    private void dispatchLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            Submission submission = queue.poll(Duration.ofSeconds(1));
            if (submission == null) {
                continue;
            }
            try {
                handle(submission);
            } catch (RuntimeException exception) {
                log.error("Submission dispatch failed: submission={} thread={} error={}",
                        submission.getSubmissionId(), submission.getThreadId(), exception.getMessage(), exception);
            }
        }
    }

    /**
     * 对单条 Submission 应用 start-or-steer 规则。
     *
     * @param submission 外部输入
     */
    void handle(Submission submission) {
        AgentThread thread = threads.require(submission.getThreadId());
        TurnSubmissionOutcome outcome = turns.startOrSteer(thread, submission);
        if (outcome.getResult() == TurnSubmissionResult.CANCELLED) {
            runtime.cancel(thread, outcome.getTurn(), submission);
            return;
        }
        if (outcome.getResult() == TurnSubmissionResult.STARTED
                || outcome.getResult() == TurnSubmissionResult.STEERED
                && outcome.getTurn().getStatus() == TurnStatus.WAITING) {
            runtimeExecutor.execute(() -> runtime.start(thread, outcome.getTurn(), submission));
        }
        log.info("[AgentSubmission] submission={} thread={} result={} turn={}", submission.getSubmissionId(),
                thread.getId(), outcome.getResult(), outcome.getTurn() == null ? null : outcome.getTurn().getId());
    }
}

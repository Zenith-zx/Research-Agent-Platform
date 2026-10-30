package com.selfinvestor.earnings.agent.runtime.submission;

import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基于 BlockingQueue 的单进程 Submission Queue。
 */
@Component
public class InMemorySubmissionQueue implements SubmissionQueue {
    @Resource
    private AgentRuntimeProperties properties;

    private BlockingQueue<Submission> queue;

    /**
     * 根据配置创建有界队列。
     */
    @PostConstruct
    void initialize() {
        if (properties.getQueueCapacity() < 1) {
            throw new IllegalStateException("Agent submission queue capacity must be at least 1");
        }
        queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    /**
     * 将 Submission 放入队列。
     *
     * @param submission 外部输入
     */
    @Override
    public void submit(Submission submission) {
        try {
            queue.put(submission);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while submitting agent input", exception);
        }
    }

    /**
     * 限时等待下一条 Submission。
     *
     * @param timeout 最长等待时间
     * @return Submission；超时时为空
     */
    @Override
    public Submission poll(Duration timeout) {
        try {
            return queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}

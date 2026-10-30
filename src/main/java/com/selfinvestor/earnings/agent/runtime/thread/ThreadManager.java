package com.selfinvestor.earnings.agent.runtime.thread;

import com.selfinvestor.earnings.repository.AgentThreadRepository;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责按投资标的获取或创建长期 Thread。
 */
@Service
public class ThreadManager {
    @Resource
    private AgentThreadRepository repository;

    private final ConcurrentHashMap<String, Object> subjectLocks = new ConcurrentHashMap<>();

    /**
     * 获取标的对应的唯一 Thread，不存在时创建。
     *
     * @param subjectType 标的类型
     * @param subjectCode 标的代码
     * @return 已存在或新建的 Thread
     */
    public AgentThread getOrCreate(SubjectType subjectType, String subjectCode) {
        String normalized = normalize(subjectCode);
        Optional<AgentThread> existing = repository.findBySubject(subjectType, normalized);
        if (existing.isPresent()) {
            return existing.get();
        }
        String key = subjectType.name() + ":" + normalized;
        synchronized (subjectLocks.computeIfAbsent(key, ignored -> new Object())) {
            try {
                return repository.findBySubject(subjectType, normalized).orElseGet(() -> insert(subjectType, normalized));
            } finally {
                subjectLocks.remove(key);
            }
        }
    }

    /**
     * 按标识读取 Thread。
     *
     * @param threadId Thread 标识
     * @return Thread
     */
    public AgentThread require(String threadId) {
        return repository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Agent thread not found: " + threadId));
    }

    /**
     * 插入新的 Thread，并兼容跨进程并发创建产生的唯一键冲突。
     *
     * @param subjectType 标的类型
     * @param subjectCode 规范化标的代码
     * @return 新增或并发创建的 Thread
     */
    private AgentThread insert(SubjectType subjectType, String subjectCode) {
        AgentThread thread = AgentThread.create(subjectType, subjectCode);
        try {
            repository.insert(thread);
            return thread;
        } catch (DuplicateKeyException exception) {
            return repository.findBySubject(subjectType, subjectCode).orElseThrow(() -> exception);
        }
    }

    /**
     * 校验并规范化投资标的代码。
     *
     * @param subjectCode 原始代码
     * @return 大写代码
     */
    private String normalize(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) {
            throw new IllegalArgumentException("Subject code is required");
        }
        return subjectCode.trim().toUpperCase(Locale.ROOT);
    }
}

package com.selfinvestor.earnings.agent.macro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.macro.model.*;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.thread.ThreadManager;
import com.selfinvestor.earnings.repository.MacroRepository;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 唯一 regime 写入口；工具没有写状态权限，模型输出必须经枚举和前态校验。
 */
@Service
public class MacroStateService {
    private static final Set<String> DIMENSIONS = Set.of(
            "growth", "inflation", "labor", "fed", "liquidity", "rates", "dollar", "risk");
    @Resource
    private MacroRepository repository;
    @Resource
    private ThreadManager threads;
    @Resource
    private ObjectMapper mapper;

    /**
     * 提供未来公司研究可复用的当前美国宏观状态读取接口。
     */
    public MacroState getCurrentUsMacroState() {
        return getOrCreate(threads.getOrCreate(SubjectType.MACRO, "US_MACRO").getId());
    }

    /**
     * 首次访问时建立 UNKNOWN 状态，已有状态保持不变。
     */
    public MacroState getOrCreate(String threadId) {
        return repository.findState(threadId).orElseGet(() -> initialize(threadId));
    }

    /**
     * 稀疏更新且验证 expected from；所有维度先验证，再执行单条带版本 SQL。
     * 无实质变化时不更新版本、时间或维度。
     */
    @Transactional
    public MacroState apply(String threadId, MacroStateChange proposed) {
        MacroState current = repository.lockState(threadId).orElseThrow();
        if (proposed == null) {
            throw new IllegalArgumentException("proposedStateChange is required; use {} for no change");
        }
        JsonNode before = mapper.valueToTree(current);
        JsonNode changes = mapper.valueToTree(proposed);
        Map<String, String> patch = new LinkedHashMap<>();
        changes.fields().forEachRemaining(entry -> {
            String dimension = entry.getKey();
            JsonNode change = entry.getValue();
            if (!DIMENSIONS.contains(dimension) || !change.hasNonNull("from")
                    || !change.hasNonNull("to") || change.path("reason").asText().isBlank()) {
                throw new IllegalArgumentException("Invalid macro state change: " + dimension);
            }
            if (!before.path(dimension).asText().equals(change.path("from").asText())) {
                throw new IllegalStateException("Stale macro state from: " + dimension);
            }
            if (!change.path("from").equals(change.path("to"))) {
                patch.put(dimension, change.path("to").asText());
            }
        });
        if (!patch.isEmpty()) {
            if (repository.patchState(threadId, current.getVersion(), patch, LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Macro state version conflict");
            }
            return repository.findState(threadId).orElseThrow();
        }
        return current;
    }

    /**
     * 并发初始化由主键保障唯一，重复创建时读取胜出的状态。
     */
    private MacroState initialize(String threadId) {
        MacroState state = new MacroState();
        state.setThreadId(threadId);
        state.setUpdatedAt(LocalDateTime.now());
        try {
            repository.insertState(state);
            return state;
        } catch (DuplicateKeyException exception) {
            return repository.findState(threadId).orElseThrow();
        }
    }
}

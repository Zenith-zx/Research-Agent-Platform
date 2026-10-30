package com.selfinvestor.earnings.agent.macro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.macro.model.MacroRelease;
import com.selfinvestor.earnings.repository.MacroRepository;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 人工或外部采集器的事实档案库；不进行分析，也不修改宏观状态。
 */
@Service
@org.springframework.validation.annotation.Validated
public class MacroReleaseService {
    @Resource
    private MacroRepository repository;
    @Resource
    private ObjectMapper mapper;

    /**
     * 按 releaseId 不可变导入；同 ID 内容不一致时拒绝覆盖原始证据。
     */
    public void store(@jakarta.validation.Valid @jakarta.validation.constraints.NotNull MacroRelease release) {
        if (release.getReleaseDate().toInstant().isAfter(java.time.Instant.now())) {
            throw new IllegalArgumentException("Future releases cannot be imported as reported facts");
        }
        String json = json(release);
        try {
            repository.insertRelease(release.getReleaseId(), release.getIndicator(), release.getPeriod(),
                    release.getReleaseDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(), json);
        } catch (DuplicateKeyException exception) {
            if (!repository.findRelease(release.getReleaseId()).orElseThrow().equals(json)) {
                throw new IllegalArgumentException("releaseId already contains different facts; use a revision ID");
            }
        }
    }

    /**
     * 按 ID 读取已导入的事实，不存在时明确报错。
     */
    public MacroRelease require(String id) {
        return parse(repository.findRelease(id).orElseThrow(() ->
                new IllegalArgumentException("No imported macro release: " + id)));
    }

    /**
     * 查询有限的已导入历史，不将空档案伪装成官方数据已齐全。
     */
    public List<MacroRelease> history(String indicator, String period, int limit) {
        return repository.releaseHistory(indicator, period, LocalDateTime.now(ZoneOffset.UTC),
                Math.max(1, Math.min(limit, 24))).stream().map(this::parse).toList();
    }

    /**
     * 将事实序列化为可审计原始输入。
     */
    public String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot encode macro data", exception);
        }
    }

    /**
     * 读取已保存且带类型的发布内容。
     */
    private MacroRelease parse(String value) {
        try {
            return mapper.readValue(value, MacroRelease.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored macro release", exception);
        }
    }
}

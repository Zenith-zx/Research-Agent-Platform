package com.selfinvestor.earnings.agent.runtime.thread;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * 一个投资标的长期存在的研究上下文。
 */
@Data
@NoArgsConstructor
public class AgentThread {
    private String id;
    private SubjectType subjectType;
    private String subjectCode;
    private String summary;
    private String memory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建新的投资研究 Thread。
     *
     * @param subjectType 标的类型
     * @param subjectCode 标的代码
     * @return 初始化后的 Thread
     */
    public static AgentThread create(SubjectType subjectType, String subjectCode) {
        AgentThread thread = new AgentThread();
        LocalDateTime now = LocalDateTime.now();
        thread.setId(UUID.randomUUID().toString());
        thread.setSubjectType(subjectType);
        thread.setSubjectCode(subjectCode.trim().toUpperCase(Locale.ROOT));
        thread.setSummary("");
        thread.setMemory("");
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        return thread;
    }
}

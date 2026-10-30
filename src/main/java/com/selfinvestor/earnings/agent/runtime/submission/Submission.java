package com.selfinvestor.earnings.agent.runtime.submission;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 所有外部触发统一进入 Runtime 的输入信封。
 */
@Data
@Builder
public class Submission {
    @Builder.Default
    private String submissionId = UUID.randomUUID().toString();
    private String threadId;
    private SubmissionType type;
    private TurnInput input;
    private String goal;
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

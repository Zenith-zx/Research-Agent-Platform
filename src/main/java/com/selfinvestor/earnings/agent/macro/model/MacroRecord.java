package com.selfinvestor.earnings.agent.macro.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 一次宏观分析的可审计持久化结果，包含稀疏变更与状态版本。
 */
@Data
public class MacroRecord {
    private String turnId;
    private String threadId;
    private String resultJson;
    private String changesJson;
    private String summary;
    private long stateVersion;
    private LocalDateTime createdAt;
}

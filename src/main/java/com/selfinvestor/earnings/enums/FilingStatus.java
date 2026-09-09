package com.selfinvestor.earnings.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财报处理状态。
 */
@Getter
@AllArgsConstructor
public enum FilingStatus {
    DETECTED("已发现"),
    DOWNLOADING("下载中"),
    DOWNLOADED("已下载"),
    PARSING("解析中"),
    PARSED("已解析"),
    ANALYZING("AI 分析中"),
    COMPLETED("处理完成"),
    FAILED("处理失败");

    /**
     * 状态描述。
     */
    private final String desc;
}

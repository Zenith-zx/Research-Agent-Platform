package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 分析完成事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisCompletedEvent {
    /**
     * Filing 主键。
     */
    private Long filingId;
    /**
     * AI 分析结果主键。
     */
    private Long analysisId;
}

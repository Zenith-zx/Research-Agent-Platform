package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 市场压力 AI 分析完成事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketStressAnalysisCompletedEvent {
    /**
     * 市场压力分析主键。
     */
    private Long analysisId;
}

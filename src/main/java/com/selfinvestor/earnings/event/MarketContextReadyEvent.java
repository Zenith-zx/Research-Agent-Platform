package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 市场上下文准备完成事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketContextReadyEvent {
    /**
     * 市场压力分析主键。
     */
    private Long analysisId;
}

package com.selfinvestor.earnings.marketstress.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 市场压力分析输入快照 DTO。
 */
@Data
@NoArgsConstructor
public class MarketSnapshotDTO {
    /**
     * 快照生成时间，统一使用 UTC。
     */
    private Instant timestamp;
    /**
     * 触发标的技术快照。
     */
    private SymbolSnapshotDTO qqq;
    /**
     * 跨资产快照。
     */
    private CrossAssetSnapshotDTO crossAsset;
}

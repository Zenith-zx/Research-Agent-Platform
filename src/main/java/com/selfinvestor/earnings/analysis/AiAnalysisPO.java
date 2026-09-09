package com.selfinvestor.earnings.analysis;

import com.selfinvestor.earnings.filing.FilingPO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AI 财报分析持久化实体。
 */
@Data
@NoArgsConstructor
public class AiAnalysisPO {
    /**
     * 分析结果主键。
     */
    private Long id;
    /**
     * 关联 Filing。
     */
    private FilingPO filing;
    /**
     * 使用的模型。
     */
    private String model;
    /**
     * 完整分析 JSON。
     */
    private String analysisJson;
    /**
     * 分析摘要。
     */
    private String summary;
    /**
     * 创建时间。
     */
    private Instant createdAt;

    /**
     * 创建 AI 分析实体。
     *
     * @param filing 关联 Filing
     * @param model 模型名称
     * @param analysisJson 分析 JSON
     * @param summary 分析摘要
     */
    public AiAnalysisPO(FilingPO filing, String model, String analysisJson, String summary) {
        this.filing = filing; this.model = model; this.analysisJson = analysisJson; this.summary = summary;
    }
}

package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.enums.EventStage;
import com.selfinvestor.earnings.enums.FinancialStress;
import com.selfinvestor.earnings.enums.FundamentalDamage;
import com.selfinvestor.earnings.enums.MarketAction;
import com.selfinvestor.earnings.enums.MarketStressStatus;
import com.selfinvestor.earnings.enums.MarketStressTriggerType;
import com.selfinvestor.earnings.enums.PricingDegree;
import com.selfinvestor.earnings.enums.Reversibility;
import com.selfinvestor.earnings.enums.SellerExhaustion;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 市场压力分析持久化对象。
 */
@Data
@NoArgsConstructor
public class MarketStressAnalysisPO {
    /**
     * 数据库主键。
     */
    private Long id;
    /**
     * 分析标的。
     */
    private String symbol;
    /**
     * 分析触发时间。
     */
    private Instant analysisTime;
    /**
     * 触发类型。
     */
    private MarketStressTriggerType triggerType;
    /**
     * RSI6。
     */
    private BigDecimal rsi6;
    /**
     * RSI14。
     */
    private BigDecimal rsi14;
    /**
     * 单日收益率。
     */
    private BigDecimal dailyReturn;
    /**
     * 五日收益率。
     */
    private BigDecimal fiveDayReturn;
    /**
     * VIX。
     */
    private BigDecimal vix;
    /**
     * 美国十年期收益率。
     */
    private BigDecimal us10y;
    /**
     * 布伦特原油。
     */
    private BigDecimal brent;
    /**
     * HYG 单日收益率。
     */
    private BigDecimal hygReturn;
    /**
     * 事件阶段。
     */
    private EventStage eventStage;
    /**
     * 事件可逆性。
     */
    private Reversibility reversibility;
    /**
     * 基本面损害。
     */
    private FundamentalDamage fundamentalDamage;
    /**
     * 金融系统压力。
     */
    private FinancialStress financialStress;
    /**
     * 事件定价程度。
     */
    private PricingDegree eventPricing;
    /**
     * 二阶影响定价程度。
     */
    private PricingDegree secondOrderPricing;
    /**
     * 卖方衰竭程度。
     */
    private SellerExhaustion sellerExhaustion;
    /**
     * 技术超卖分数。
     */
    private Integer technicalScore;
    /**
     * 均值回归分数。
     */
    private Integer meanReversionScore;
    /**
     * 研究行动。
     */
    private MarketAction action;
    /**
     * 模型置信度。
     */
    private BigDecimal confidence;
    /**
     * 当前市场正在交易的核心因素。
     */
    private String marketIsTrading;
    /**
     * 分析依据摘要。
     */
    private String reasoningSummary;
    /**
     * 触发时完整市场快照 JSON。
     */
    private String marketSnapshotJson;
    /**
     * 触发时新闻上下文 JSON。
     */
    private String newsJson;
    /**
     * 完整结构化分析 JSON。
     */
    private String analysisJson;
    /**
     * 任务状态。
     */
    private MarketStressStatus status;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 创建时间。
     */
    private Instant createdAt;
    /**
     * 更新时间。
     */
    private Instant updatedAt;

    /**
     * 创建超卖检测记录。
     *
     * @param symbol 标的
     * @param analysisTime 分析时间
     * @param technicalSnapshot 技术快照
     */
    public MarketStressAnalysisPO(String symbol, Instant analysisTime, SymbolSnapshotDTO technicalSnapshot) {
        this.symbol = symbol;
        this.analysisTime = analysisTime;
        this.triggerType = MarketStressTriggerType.RSI_OVERSOLD;
        this.rsi6 = technicalSnapshot.getRsi6();
        this.rsi14 = technicalSnapshot.getRsi14();
        this.dailyReturn = technicalSnapshot.getDailyReturn();
        this.fiveDayReturn = technicalSnapshot.getFiveDayReturn();
        this.status = MarketStressStatus.DETECTED;
    }

    /**
     * 将结构化分析中的查询字段复制到持久化对象。
     *
     * @param analysis 结构化分析
     */
    public void complete(MarketStressAnalysisDTO analysis) {
        this.eventStage = analysis.getEventStage();
        this.reversibility = analysis.getReversibility();
        this.fundamentalDamage = analysis.getFundamentalDamage();
        this.financialStress = analysis.getFinancialStress();
        this.eventPricing = analysis.getEventPricing();
        this.secondOrderPricing = analysis.getSecondOrderEffectsPricing();
        this.sellerExhaustion = analysis.getSellerExhaustion();
        this.technicalScore = analysis.getTechnicalScore();
        this.meanReversionScore = analysis.getMeanReversionScore();
        this.action = analysis.getAction();
        this.confidence = analysis.getConfidence();
        this.marketIsTrading = analysis.getMarketIsTrading();
        this.reasoningSummary = analysis.getReasoningSummary();
        this.status = MarketStressStatus.COMPLETED;
    }
}

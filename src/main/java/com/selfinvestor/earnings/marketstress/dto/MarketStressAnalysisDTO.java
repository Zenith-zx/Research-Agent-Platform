package com.selfinvestor.earnings.marketstress.dto;

import com.selfinvestor.earnings.enums.EventStage;
import com.selfinvestor.earnings.enums.FinancialStress;
import com.selfinvestor.earnings.enums.FundamentalDamage;
import com.selfinvestor.earnings.enums.MarketAction;
import com.selfinvestor.earnings.enums.MarketRegime;
import com.selfinvestor.earnings.enums.NegativeNewsSensitivity;
import com.selfinvestor.earnings.enums.PricingDegree;
import com.selfinvestor.earnings.enums.Reversibility;
import com.selfinvestor.earnings.enums.SellerExhaustion;
import com.selfinvestor.earnings.enums.TechnicalCondition;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构化市场压力分析 DTO。
 */
@Data
@NoArgsConstructor
public class MarketStressAnalysisDTO {
    /**
     * 触发标的。
     */
    private String symbol;
    /**
     * 分析时间。
     */
    private Instant analysisTimestamp;
    /**
     * 市场环境。
     */
    private MarketRegime marketRegime;
    /**
     * 主要市场驱动因素。
     */
    private List<PrimaryDriverDTO> primaryDrivers = new ArrayList<>();
    /**
     * 事件所处阶段。
     */
    private EventStage eventStage;
    /**
     * 事件可逆性。
     */
    private Reversibility reversibility;
    /**
     * 基本面损害程度。
     */
    private FundamentalDamage fundamentalDamage;
    /**
     * 金融系统压力。
     */
    private FinancialStress financialStress;
    /**
     * 事件本身定价程度。
     */
    private PricingDegree eventPricing;
    /**
     * 二阶影响定价程度。
     */
    private PricingDegree secondOrderEffectsPricing;
    /**
     * 市场对新增坏消息的敏感度变化。
     */
    private NegativeNewsSensitivity negativeNewsSensitivity;
    /**
     * 卖方衰竭程度。
     */
    private SellerExhaustion sellerExhaustion;
    /**
     * 技术状态。
     */
    private TechnicalCondition technicalCondition;
    /**
     * Java 计算的技术超卖分数。
     */
    private Integer technicalScore;
    /**
     * Java 综合计算的均值回归分数。
     */
    private Integer meanReversionScore;
    /**
     * 研究行动分类，不代表自动交易指令。
     */
    private MarketAction action;
    /**
     * 当前市场正在交易的核心叙事。
     */
    private String marketIsTrading;
    /**
     * 分析依据摘要。
     */
    private String reasoningSummary;
    /**
     * 重新评估前需要观察的确认信号。
     */
    private List<String> confirmationSignals = new ArrayList<>();
    /**
     * 当前均值回归逻辑失效的信号。
     */
    private List<String> invalidationSignals = new ArrayList<>();
    /**
     * 当前关键风险。
     */
    private List<String> keyRisks = new ArrayList<>();
    /**
     * 模型判断置信度，范围为零到一。
     */
    private BigDecimal confidence;
}

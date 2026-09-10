package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.enums.FinancialStress;
import com.selfinvestor.earnings.enums.FundamentalDamage;
import com.selfinvestor.earnings.enums.MarketAction;
import com.selfinvestor.earnings.enums.PricingDegree;
import com.selfinvestor.earnings.enums.Reversibility;
import com.selfinvestor.earnings.enums.SellerExhaustion;
import com.selfinvestor.earnings.enums.TechnicalCondition;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 可解释的均值回归评分服务。
 */
@Service
public class MeanReversionScoreService {
    @Resource
    private MarketStressProperties properties;

    /**
     * 使用 Java 技术指标与 GPT 分类结果计算最终评分和行动。
     *
     * @param snapshot 技术快照
     * @param analysis GPT 分类结果
     * @return 写入最终分数和行动的分析结果
     */
    public MarketStressAnalysisDTO apply(SymbolSnapshotDTO snapshot, MarketStressAnalysisDTO analysis) {
        int technicalScore = technicalScore(snapshot);
        MarketStressProperties.Scoring weights = properties.getScoring();
        int weightedSum = technicalScore * weights.getTechnicalOversoldWeight()
                + reversibilityScore(analysis.getReversibility()) * weights.getEventReversibilityWeight()
                + fundamentalScore(analysis.getFundamentalDamage()) * weights.getFundamentalDamageWeight()
                + financialScore(analysis.getFinancialStress()) * weights.getFinancialStressWeight()
                + pricingScore(analysis.getEventPricing(), analysis.getSecondOrderEffectsPricing()) * weights.getBadNewsPricingWeight()
                + exhaustionScore(analysis.getSellerExhaustion()) * weights.getSellerExhaustionWeight();
        int totalWeight = weights.getTechnicalOversoldWeight() + weights.getEventReversibilityWeight()
                + weights.getFundamentalDamageWeight() + weights.getFinancialStressWeight()
                + weights.getBadNewsPricingWeight() + weights.getSellerExhaustionWeight();
        if (totalWeight <= 0) {
            throw new IllegalStateException("Mean reversion scoring weights must be greater than zero");
        }
        int score = Math.max(0, Math.min(100, Math.round((float) weightedSum / totalWeight)));
        analysis.setTechnicalScore(technicalScore);
        analysis.setTechnicalCondition(technicalScore >= 85 ? TechnicalCondition.EXTREME_OVERSOLD
                : technicalScore >= 60 ? TechnicalCondition.OVERSOLD : TechnicalCondition.NORMAL);
        analysis.setMeanReversionScore(score);
        analysis.setAction(action(score, analysis));
        return analysis;
    }

    /**
     * 计算零到一百的技术超卖分数。
     *
     * @param snapshot 技术快照
     * @return 技术分数
     */
    public int technicalScore(SymbolSnapshotDTO snapshot) {
        List<Integer> components = new ArrayList<>();
        add(components, inverseLinear(snapshot.getRsi6(), BigDecimal.valueOf(50), BigDecimal.valueOf(20)));
        add(components, inverseLinear(snapshot.getRsi14(), BigDecimal.valueOf(55), BigDecimal.valueOf(25)));
        add(components, declineScore(snapshot.getFiveDayReturn(), new BigDecimal("-10")));
        add(components, declineScore(snapshot.getDistanceFromMa20(), new BigDecimal("-10")));
        add(components, declineScore(snapshot.getDistanceFromMa50(), new BigDecimal("-15")));
        if (CollectionUtils.isEmpty(components)) {
            return 0;
        }
        return (int) Math.round(components.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    /**
     * 添加存在的评分组件。
     *
     * @param scores 分数组件
     * @param score 可空分数
     */
    private void add(List<Integer> scores, Integer score) {
        if (score != null) {
            scores.add(score);
        }
    }

    /**
     * 将越低越超卖的指标映射到零到一百。
     *
     * @param value 指标值
     * @param neutral 中性值
     * @param extreme 极端值
     * @return 分数
     */
    private Integer inverseLinear(BigDecimal value, BigDecimal neutral, BigDecimal extreme) {
        if (value == null) {
            return null;
        }
        BigDecimal raw = neutral.subtract(value).multiply(BigDecimal.valueOf(100))
                .divide(neutral.subtract(extreme), 4, RoundingMode.HALF_UP);
        return clamp(raw);
    }

    /**
     * 将负收益或负均线距离映射到零到一百。
     *
     * @param value 当前值
     * @param extreme 极端下跌值
     * @return 分数
     */
    private Integer declineScore(BigDecimal value, BigDecimal extreme) {
        if (value == null) {
            return null;
        }
        return clamp(value.multiply(BigDecimal.valueOf(100)).divide(extreme, 4, RoundingMode.HALF_UP));
    }

    /**
     * 将分数限制在零到一百。
     *
     * @param value 原始分数
     * @return 整数分数
     */
    private int clamp(BigDecimal value) {
        return Math.max(0, Math.min(100, value.setScale(0, RoundingMode.HALF_UP).intValue()));
    }

    /**
     * 映射事件可逆性分数。
     *
     * @param value 可逆性
     * @return 分数
     */
    private int reversibilityScore(Reversibility value) {
        if (value == Reversibility.HIGH) {
            return 100;
        }
        if (value == Reversibility.MEDIUM) {
            return 60;
        }
        if (value == Reversibility.LOW) {
            return 10;
        }
        return 40;
    }

    /**
     * 映射基本面损害分数。
     *
     * @param value 基本面损害
     * @return 分数
     */
    private int fundamentalScore(FundamentalDamage value) {
        if (value == FundamentalDamage.NONE) {
            return 100;
        }
        if (value == FundamentalDamage.LIMITED) {
            return 75;
        }
        if (value == FundamentalDamage.MODERATE) {
            return 35;
        }
        if (value == FundamentalDamage.SEVERE) {
            return 0;
        }
        return 40;
    }

    /**
     * 映射金融压力分数。
     *
     * @param value 金融压力
     * @return 分数
     */
    private int financialScore(FinancialStress value) {
        if (value == FinancialStress.LOW) {
            return 100;
        }
        if (value == FinancialStress.MODERATE) {
            return 75;
        }
        if (value == FinancialStress.HIGH) {
            return 30;
        }
        if (value == FinancialStress.SYSTEMIC) {
            return 0;
        }
        return 40;
    }

    /**
     * 组合事件与二阶影响定价分数，避免只看事件本身。
     *
     * @param event 事件定价
     * @param secondOrder 二阶影响定价
     * @return 平均分数
     */
    private int pricingScore(PricingDegree event, PricingDegree secondOrder) {
        return Math.round((pricingValue(event) + pricingValue(secondOrder)) / 2.0f);
    }

    /**
     * 映射定价程度分数。
     *
     * @param value 定价程度
     * @return 分数
     */
    private int pricingValue(PricingDegree value) {
        if (value == PricingDegree.NOT_PRICED) {
            return 0;
        }
        if (value == PricingDegree.PARTIALLY_PRICED) {
            return 40;
        }
        if (value == PricingDegree.MOSTLY_PRICED) {
            return 75;
        }
        if (value == PricingDegree.HIGH_DEGREE_OF_PRICING) {
            return 90;
        }
        return 30;
    }

    /**
     * 映射卖方衰竭分数。
     *
     * @param value 卖方衰竭
     * @return 分数
     */
    private int exhaustionScore(SellerExhaustion value) {
        if (value == SellerExhaustion.NONE) {
            return 0;
        }
        if (value == SellerExhaustion.WEAK) {
            return 30;
        }
        if (value == SellerExhaustion.MODERATE) {
            return 65;
        }
        if (value == SellerExhaustion.STRONG) {
            return 100;
        }
        return 30;
    }

    /**
     * 根据评分和风险护栏生成最终研究行动。
     *
     * @param score 均值回归分数
     * @param analysis 分类结果
     * @return 研究行动
     */
    private MarketAction action(int score, MarketStressAnalysisDTO analysis) {
        if (analysis.getFinancialStress() == FinancialStress.SYSTEMIC
                || analysis.getFundamentalDamage() == FundamentalDamage.SEVERE) {
            return MarketAction.AVOID_BUYING_THE_DIP;
        }
        if (analysis.getFinancialStress() == FinancialStress.HIGH && score < 70) {
            return MarketAction.HIGH_RISK;
        }
        if (score >= 85) {
            return MarketAction.STRONG_MEAN_REVERSION_SETUP;
        }
        if (score >= 70) {
            return MarketAction.SCALE_IN;
        }
        if (score >= 50) {
            return MarketAction.WAIT_FOR_CONFIRMATION;
        }
        if (score >= 30) {
            return MarketAction.HIGH_RISK;
        }
        return MarketAction.AVOID_BUYING_THE_DIP;
    }
}

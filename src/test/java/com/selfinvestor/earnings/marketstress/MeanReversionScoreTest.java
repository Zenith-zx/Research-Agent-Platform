package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.enums.FinancialStress;
import com.selfinvestor.earnings.enums.FundamentalDamage;
import com.selfinvestor.earnings.enums.MarketAction;
import com.selfinvestor.earnings.enums.PricingDegree;
import com.selfinvestor.earnings.enums.Reversibility;
import com.selfinvestor.earnings.enums.SellerExhaustion;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import com.selfinvestor.earnings.service.MeanReversionScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MeanReversionScoreTest {
    /**
     * 验证 Java 综合评分使用配置权重并产生分批进入分类。
     */
    @Test
    void calculatesConfiguredMeanReversionScore() {
        MeanReversionScoreService service = new MeanReversionScoreService();
        ReflectionTestUtils.setField(service, "properties", new MarketStressProperties());
        SymbolSnapshotDTO snapshot = new SymbolSnapshotDTO();
        snapshot.setRsi6(new BigDecimal("20"));
        snapshot.setRsi14(new BigDecimal("28"));
        snapshot.setFiveDayReturn(new BigDecimal("-9"));
        snapshot.setDistanceFromMa20(new BigDecimal("-10"));
        snapshot.setDistanceFromMa50(new BigDecimal("-12"));
        MarketStressAnalysisDTO analysis = new MarketStressAnalysisDTO();
        analysis.setReversibility(Reversibility.HIGH);
        analysis.setFundamentalDamage(FundamentalDamage.LIMITED);
        analysis.setFinancialStress(FinancialStress.LOW);
        analysis.setEventPricing(PricingDegree.MOSTLY_PRICED);
        analysis.setSecondOrderEffectsPricing(PricingDegree.PARTIALLY_PRICED);
        analysis.setSellerExhaustion(SellerExhaustion.MODERATE);
        service.apply(snapshot, analysis);
        assertThat(analysis.getTechnicalScore()).isGreaterThanOrEqualTo(80);
        assertThat(analysis.getMeanReversionScore()).isBetween(70, 84);
        assertThat(analysis.getAction()).isEqualTo(MarketAction.SCALE_IN);
    }

    /**
     * 验证系统性压力会覆盖高技术超卖分数。
     */
    @Test
    void systemicStressPreventsDipBuyingAction() {
        MeanReversionScoreService service = new MeanReversionScoreService();
        ReflectionTestUtils.setField(service, "properties", new MarketStressProperties());
        MarketStressAnalysisDTO analysis = new MarketStressAnalysisDTO();
        analysis.setReversibility(Reversibility.HIGH);
        analysis.setFundamentalDamage(FundamentalDamage.NONE);
        analysis.setFinancialStress(FinancialStress.SYSTEMIC);
        analysis.setEventPricing(PricingDegree.HIGH_DEGREE_OF_PRICING);
        analysis.setSecondOrderEffectsPricing(PricingDegree.HIGH_DEGREE_OF_PRICING);
        analysis.setSellerExhaustion(SellerExhaustion.STRONG);
        service.apply(extremeSnapshot(), analysis);
        assertThat(analysis.getAction()).isEqualTo(MarketAction.AVOID_BUYING_THE_DIP);
    }

    /**
     * 创建极端超卖快照。
     *
     * @return 测试快照
     */
    private SymbolSnapshotDTO extremeSnapshot() {
        SymbolSnapshotDTO snapshot = new SymbolSnapshotDTO();
        snapshot.setRsi6(BigDecimal.TEN);
        snapshot.setRsi14(new BigDecimal("20"));
        snapshot.setFiveDayReturn(new BigDecimal("-15"));
        snapshot.setDistanceFromMa20(new BigDecimal("-15"));
        snapshot.setDistanceFromMa50(new BigDecimal("-20"));
        return snapshot;
    }
}

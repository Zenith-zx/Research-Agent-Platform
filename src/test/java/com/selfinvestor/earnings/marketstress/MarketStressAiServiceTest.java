package com.selfinvestor.earnings.marketstress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.enums.FinancialStress;
import com.selfinvestor.earnings.enums.FundamentalDamage;
import com.selfinvestor.earnings.enums.PricingDegree;
import com.selfinvestor.earnings.enums.Reversibility;
import com.selfinvestor.earnings.enums.SellerExhaustion;
import com.selfinvestor.earnings.marketstress.dto.MarketSnapshotDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import com.selfinvestor.earnings.service.MarketStressAiService;
import com.selfinvestor.earnings.service.MeanReversionScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketStressAiServiceTest {
    /**
     * 验证市场上下文发送给共用 OpenAI 客户端并由 Java 覆盖最终评分。
     */
    @Test
    void sendsStructuredContextAndAppliesJavaScore() {
        OpenAiClient client = mock(OpenAiClient.class);
        MarketStressAnalysisDTO modelResult = new MarketStressAnalysisDTO();
        modelResult.setReversibility(Reversibility.MEDIUM);
        modelResult.setFundamentalDamage(FundamentalDamage.LIMITED);
        modelResult.setFinancialStress(FinancialStress.LOW);
        modelResult.setEventPricing(PricingDegree.MOSTLY_PRICED);
        modelResult.setSecondOrderEffectsPricing(PricingDegree.PARTIALLY_PRICED);
        modelResult.setSellerExhaustion(SellerExhaustion.MODERATE);
        when(client.analyzeMarketStress(contains("QQQ"))).thenReturn(modelResult);

        MeanReversionScoreService scoreService = new MeanReversionScoreService();
        ReflectionTestUtils.setField(scoreService, "properties", new MarketStressProperties());
        MarketStressAiService service = new MarketStressAiService();
        ReflectionTestUtils.setField(service, "openAiClient", client);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(service, "scoreService", scoreService);
        MarketStressAnalysisDTO result = service.analyze(snapshot(), Collections.emptyList());
        assertThat(result.getSymbol()).isEqualTo("QQQ");
        assertThat(result.getMeanReversionScore()).isNotNull();
        verify(client).analyzeMarketStress(contains("QQQ"));
    }

    /**
     * 创建测试市场快照。
     *
     * @return 市场快照
     */
    private MarketSnapshotDTO snapshot() {
        SymbolSnapshotDTO qqq = new SymbolSnapshotDTO();
        qqq.setSymbol("QQQ");
        qqq.setRsi6(new BigDecimal("25"));
        qqq.setRsi14(new BigDecimal("30"));
        qqq.setFiveDayReturn(new BigDecimal("-8"));
        qqq.setDistanceFromMa20(new BigDecimal("-8"));
        qqq.setDistanceFromMa50(new BigDecimal("-10"));
        MarketSnapshotDTO snapshot = new MarketSnapshotDTO();
        snapshot.setTimestamp(Instant.parse("2026-01-01T15:00:00Z"));
        snapshot.setQqq(qqq);
        return snapshot;
    }
}

package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.repository.MarketStressAnalysisRepository;
import com.selfinvestor.earnings.service.MarketStressCooldownService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CooldownTest {
    /**
     * 验证同一标的十二小时内不会重复分析。
     */
    @Test
    void preventsRepeatedAnalysisWithinCooldown() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        MarketStressAnalysisRepository repository = mock(MarketStressAnalysisRepository.class);
        when(repository.findLatestActiveAnalysisTime("QQQ"))
                .thenReturn(Optional.of(Instant.parse("2026-01-01T10:00:00Z")));
        MarketStressProperties properties = new MarketStressProperties();
        properties.setAnalysisCooldownHours(12);
        MarketStressCooldownService service = new MarketStressCooldownService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "retriggerPolicy", new DefaultMarketStressRetriggerPolicy());
        assertThat(service.isCoolingDown("QQQ", now)).isTrue();
        assertThat(service.isCoolingDown("QQQ", Instant.parse("2026-01-02T00:01:00Z"))).isFalse();
    }
}

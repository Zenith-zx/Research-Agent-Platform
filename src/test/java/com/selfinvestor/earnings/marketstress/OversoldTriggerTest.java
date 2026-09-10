package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.service.OversoldTriggerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OversoldTriggerTest {
    /**
     * 验证 RSI 低于配置阈值时触发。
     */
    @Test
    void triggersBelowConfiguredThreshold() {
        MarketStressProperties properties = new MarketStressProperties();
        properties.setRsiTrigger(new BigDecimal("30"));
        OversoldTriggerService service = new OversoldTriggerService();
        ReflectionTestUtils.setField(service, "properties", properties);
        assertThat(service.shouldTrigger(Optional.of(new BigDecimal("29.99")))).isTrue();
        assertThat(service.shouldTrigger(Optional.of(new BigDecimal("30.00")))).isFalse();
        assertThat(service.shouldTrigger(Optional.empty())).isFalse();
    }
}

package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.monitor.MarketStressScheduler;
import com.selfinvestor.earnings.service.MarketStressService;
import com.selfinvestor.earnings.service.UsMarketSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SchedulerTest {
    /**
     * 验证周末不会调用市场扫描。
     */
    @Test
    void doesNotScanOutsideTradingSession() {
        MarketStressProperties properties = new MarketStressProperties();
        MarketStressService service = mock(MarketStressService.class);
        UsMarketSessionService session = new UsMarketSessionService();
        ReflectionTestUtils.setField(session, "clock",
                Clock.fixed(Instant.parse("2026-01-03T15:00:00Z"), ZoneOffset.UTC));
        MarketStressScheduler scheduler = new MarketStressScheduler();
        ReflectionTestUtils.setField(scheduler, "properties", properties);
        ReflectionTestUtils.setField(scheduler, "marketStressService", service);
        ReflectionTestUtils.setField(scheduler, "marketSessionService", session);
        scheduler.scan();
        verify(service, never()).scan("QQQ");
    }

    /**
     * 验证纽约时区在冬夏令时均正确识别上午十点。
     */
    @Test
    void handlesNewYorkDaylightSavingTime() {
        UsMarketSessionService service = new UsMarketSessionService();
        assertThat(service.isRegularSession(Instant.parse("2026-01-06T15:00:00Z"))).isTrue();
        assertThat(service.isRegularSession(Instant.parse("2026-07-06T14:00:00Z"))).isTrue();
        assertThat(service.isRegularSession(Instant.parse("2026-07-06T21:00:00Z"))).isFalse();
    }
}

package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.repository.DailyMarketCloseRepository;
import com.selfinvestor.earnings.service.DailyMarketCloseService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DailyMarketCloseRepositoryTest {
    @Resource
    private DailyMarketCloseService service;
    @Resource
    private DailyMarketCloseRepository repository;

    /**
     * 验证上一交易日收盘价只保存一次并可按日期升序读取。
     */
    @Test
    void recordsPreviousCloseOnceAndReadsItAsDailyBar() {
        MarketQuoteDTO quote = new MarketQuoteDTO();
        quote.setSymbol("QQQ_CLOSE_TEST");
        quote.setPreviousClose(new BigDecimal("607.7500"));
        quote.setTimestamp(Instant.parse("2026-09-11T14:00:00Z"));
        quote.setSource("Finnhub Quote API");

        boolean first = service.recordPreviousClose("QQQ_CLOSE_TEST", quote);
        boolean duplicate = service.recordPreviousClose("QQQ_CLOSE_TEST", quote);
        List<DailyBarDTO> bars = service.findLatestBars("QQQ_CLOSE_TEST", 20);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).getDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(bars.get(0).getClose()).isEqualByComparingTo("607.7500");
        assertThat(repository.findLatest("QQQ_CLOSE_TEST", 20)).hasSize(1);
    }
}

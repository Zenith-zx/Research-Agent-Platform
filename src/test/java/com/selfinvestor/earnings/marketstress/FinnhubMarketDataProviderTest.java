package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.provider.FinnhubClient;
import com.selfinvestor.earnings.marketstress.provider.FinnhubMarketDataProvider;
import com.selfinvestor.earnings.service.DailyMarketCloseService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinnhubMarketDataProviderTest {
    private MockWebServer server;
    private DailyMarketCloseService dailyMarketCloseService;
    private FinnhubMarketDataProvider provider;

    /**
     * 创建模拟 Finnhub 服务和行情 Provider。
     *
     * @throws IOException 模拟服务器启动失败
     */
    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        MarketStressProperties properties = new MarketStressProperties();
        properties.getProvider().setBaseUrl(server.url("/api/v1").toString());
        properties.getProvider().setApiKey("test-finnhub-key");

        FinnhubClient client = new FinnhubClient();
        ReflectionTestUtils.setField(client, "webClientBuilder", WebClient.builder());
        ReflectionTestUtils.setField(client, "properties", properties);
        client.initialize();

        dailyMarketCloseService = mock(DailyMarketCloseService.class);
        provider = new FinnhubMarketDataProvider();
        ReflectionTestUtils.setField(provider, "client", client);
        ReflectionTestUtils.setField(provider, "dailyMarketCloseService", dailyMarketCloseService);
        ReflectionTestUtils.setField(provider, "properties", properties);
    }

    /**
     * 关闭模拟 Finnhub 服务。
     *
     * @throws IOException 模拟服务器关闭失败
     */
    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * 验证 Quote API 字段映射并确认 API Key 只通过请求头发送。
     *
     * @throws InterruptedException 读取请求被中断
     */
    @Test
    void mapsFinnhubQuoteAndUsesApiKeyHeader() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"c\":600.25,\"d\":-7.50,\"dp\":-1.234,\"h\":608,"
                        + "\"l\":598,\"o\":607,\"pc\":607.75,\"t\":1789135200}"));

        MarketQuoteDTO quote = provider.getQuote("qqq");

        assertThat(quote.getSymbol()).isEqualTo("QQQ");
        assertThat(quote.getPrice()).isEqualByComparingTo("600.25");
        assertThat(quote.getPreviousClose()).isEqualByComparingTo("607.75");
        assertThat(quote.getDailyReturn()).isEqualByComparingTo("-1.234");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/quote?symbol=QQQ");
        assertThat(request.getHeader("X-Finnhub-Token")).isEqualTo("test-finnhub-key");
        assertThat(request.getPath()).doesNotContain("test-finnhub-key");
    }

    /**
     * 验证历史日线完全从 MySQL 收盘价服务读取，不调用 Premium Candle API。
     */
    @Test
    void readsDailyBarsFromDatabaseOnly() {
        DailyBarDTO bar = new DailyBarDTO("QQQ", LocalDate.of(2026, 9, 10),
                new BigDecimal("607.75"), new BigDecimal("607.75"), new BigDecimal("607.75"),
                new BigDecimal("607.75"), null, Instant.parse("2026-09-10T20:00:00Z"), "Finnhub Quote API");
        when(dailyMarketCloseService.findLatestBars("QQQ", 20)).thenReturn(Collections.singletonList(bar));

        List<DailyBarDTO> bars = provider.getDailyBars("qqq", 20);

        assertThat(bars).hasSize(1);
        verify(dailyMarketCloseService).findLatestBars("QQQ", 20);
        assertThat(server.getRequestCount()).isZero();
    }
}

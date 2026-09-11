package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.provider.IbkrMarketDataProvider;
import com.selfinvestor.earnings.marketstress.provider.IbkrWebApiClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IbkrMarketDataProviderTest {
    private MockWebServer server;
    private MarketStressProperties properties;
    private IbkrMarketDataProvider provider;

    /**
     * 创建模拟 IBKR Client Portal Gateway。
     *
     * @throws IOException 模拟服务器启动失败
     */
    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        properties = new MarketStressProperties();
        properties.getProvider().setMarketData("ibkr");
        properties.getProvider().getIbkr().setBaseUrl(server.url("/v1/api").toString());
        properties.getProvider().getIbkr().setVerifySsl(true);
        properties.getProvider().getIbkr().setSnapshotPreflightDelay(Duration.ZERO);

        IbkrWebApiClient client = new IbkrWebApiClient();
        ReflectionTestUtils.setField(client, "webClientBuilder", WebClient.builder());
        ReflectionTestUtils.setField(client, "properties", properties);
        client.initialize();

        provider = new IbkrMarketDataProvider();
        ReflectionTestUtils.setField(provider, "client", client);
        ReflectionTestUtils.setField(provider, "properties", properties);
    }

    /**
     * 关闭模拟服务器。
     *
     * @throws IOException 模拟服务器关闭失败
     */
    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * 验证标准 top-of-book 接口会先预热再返回实时行情，且不会调用监管快照。
     *
     * @throws InterruptedException 读取请求被中断
     */
    @Test
    void getsRealtimeQuoteWithoutRegulatorySnapshot() throws InterruptedException {
        enqueueAuthenticatedSession();
        enqueueJson("{\"QQQ\":[{\"assetClass\":\"STK\",\"contracts\":["
                + "{\"conid\":320227571,\"exchange\":\"NASDAQ\",\"isUS\":true}]}]}");
        enqueueJson("[{\"conid\":320227571,\"conidEx\":\"320227571\"}]");
        enqueueJson("[{\"conid\":320227571,\"31\":\"C600.25\",\"83\":\"-1.25%\","
                + "\"6509\":\"RpB\",\"_updated\":1789056000000}]");

        MarketQuoteDTO quote = provider.getQuote("qqq");

        assertThat(quote.getSymbol()).isEqualTo("QQQ");
        assertThat(quote.getPrice()).isEqualByComparingTo("600.25");
        assertThat(quote.getDailyReturn()).isEqualByComparingTo("-1.25");
        assertThat(server.getRequestCount()).isEqualTo(4);
        for (int index = 0; index < 4; index++) {
            RecordedRequest request = server.takeRequest();
            assertThat(request.getPath()).doesNotContain("regsnapshot");
        }
    }

    /**
     * 验证开启实时行情保护时拒绝 IBKR 延迟行情。
     */
    @Test
    void rejectsDelayedQuoteWhenRealtimeIsRequired() {
        enqueueAuthenticatedSession();
        enqueueJson("{\"QQQ\":[{\"assetClass\":\"STK\",\"contracts\":["
                + "{\"conid\":320227571,\"exchange\":\"NASDAQ\",\"isUS\":true}]}]}");
        enqueueJson("[{\"conid\":320227571,\"31\":\"600.25\",\"6509\":\"DpB\"}]");

        assertThatThrownBy(() -> provider.getQuote("QQQ"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("real-time market data is unavailable");
    }

    /**
     * 验证历史日线转换以及同一交易日的缓存复用。
     */
    @Test
    void getsAndCachesDailyBars() {
        enqueueAuthenticatedSession();
        enqueueJson("{\"QQQ\":[{\"assetClass\":\"STK\",\"contracts\":["
                + "{\"conid\":320227571,\"exchange\":\"NASDAQ\",\"isUS\":true}]}]}");
        StringBuilder data = new StringBuilder("{\"data\":[");
        for (int index = 0; index < 220; index++) {
            if (index > 0) {
                data.append(',');
            }
            data.append("{\"o\":100,\"h\":102,\"l\":99,\"c\":101,\"v\":1000,\"t\":")
                    .append(1767225600000L + index * 86400000L).append('}');
        }
        data.append("]}");
        enqueueJson(data.toString());

        List<DailyBarDTO> first = provider.getDailyBars("QQQ", 220);
        List<DailyBarDTO> second = provider.getDailyBars("QQQ", 10);

        assertThat(first).hasSize(220);
        assertThat(second).hasSize(10);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    /**
     * 加入已认证会话响应。
     */
    private void enqueueAuthenticatedSession() {
        enqueueJson("{\"authenticated\":true,\"connected\":true,\"competing\":false}");
    }

    /**
     * 加入 JSON 模拟响应。
     *
     * @param body JSON 响应体
     */
    private void enqueueJson(String body) {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }
}

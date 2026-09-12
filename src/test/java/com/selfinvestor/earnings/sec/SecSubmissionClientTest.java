package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecSubmissionClientTest {

    private MockWebServer server;

    /**
     * 启动模拟 SEC 服务。
     *
     * @throws Exception 服务启动失败时抛出
     */
    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /**
     * 关闭模拟 SEC 服务。
     *
     * @throws Exception 服务关闭失败时抛出
     */
    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    /**
     * 验证客户端能够自动解压并解析 SEC 返回的 GZIP JSON。
     *
     * @throws Exception 压缩或请求失败时抛出
     */
    @Test
    void decompressesGzipSubmissionResponse() throws Exception {
        String responseJson = "{\"filings\":{\"recent\":{"
                + "\"accessionNumber\":[\"0001045810-26-000001\"],"
                + "\"filingDate\":[\"2026-09-11\"],"
                + "\"acceptanceDateTime\":[\"2026-09-11T16:00:00Z\"],"
                + "\"form\":[\"8-K\"],"
                + "\"primaryDocument\":[\"nvda-20260911.htm\"],"
                + "\"items\":[\"2.02,9.01\"]}}}";
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Encoding", "gzip")
                .setBody(new Buffer().write(gzip(responseJson))));

        SecSubmissionClient client = createClient();
        CompanyPO company = new CompanyPO("NVDA", "0001045810", "NVIDIA Corporation", true);

        List<SecFilingMetadataDTO> filings = client.recentFilings(company);

        assertThat(filings).hasSize(1);
        assertThat(filings.get(0).getAccessionNumber()).isEqualTo("0001045810-26-000001");
        assertThat(server.takeRequest().getHeader("Accept-Encoding")).contains("gzip");
    }

    /**
     * 创建连接模拟 SEC 服务的客户端。
     *
     * @return SEC submissions 客户端
     */
    private SecSubmissionClient createClient() {
        EarningsProperties properties = new EarningsProperties();
        properties.getSec().setBaseUrl(server.url("/").toString());
        properties.getSec().setDataBaseUrl(server.url("/").toString());
        properties.getSec().setUserAgent("self-investor-test test@example.com");
        properties.getSec().setConnectTimeout(Duration.ofSeconds(2));
        properties.getSec().setReadTimeout(Duration.ofSeconds(2));

        SecSubmissionClient client = new SecSubmissionClient();
        ReflectionTestUtils.setField(client, "webClientBuilder", WebClient.builder());
        ReflectionTestUtils.setField(client, "properties", properties);
        ReflectionTestUtils.setField(client, "rateLimiter", mock(SecRateLimiter.class));
        client.initialize();
        return client;
    }

    /**
     * 将 JSON 文本压缩为 GZIP 字节。
     *
     * @param content JSON 文本
     * @return GZIP 字节
     * @throws Exception 压缩失败时抛出
     */
    private byte[] gzip(String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}

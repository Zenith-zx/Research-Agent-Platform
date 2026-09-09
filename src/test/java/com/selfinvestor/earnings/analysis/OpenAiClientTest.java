package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.config.EarningsProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientTest {
    private MockWebServer server;

    /**
     * 启动模拟 OpenAI 服务。
     *
     * @throws Exception 服务启动失败时抛出
     */
    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /**
     * 关闭模拟 OpenAI 服务。
     *
     * @throws Exception 服务关闭失败时抛出
     */
    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    /**
     * 验证 Responses Structured Output 请求及 DTO 反序列化。
     *
     * @throws Exception 测试执行失败时抛出
     */
    @Test
    void usesResponsesStructuredOutputAndDeserializesDto() throws Exception {
        String analysis = "{\"company\":\"Credo\",\"ticker\":\"CRDO\",\"fiscalQuarter\":null,\"headline\":\"Strong\",\"financials\":null,\"guidance\":null,\"segments\":[],\"positives\":[],\"negatives\":[],\"risks\":[],\"managementCommentary\":[],\"fundamentalTrend\":\"IMPROVING\",\"fundamentalTrendReason\":\"Growth\",\"investmentThesisChanged\":false,\"investmentThesisChangeReason\":null,\"consensusComparisonAvailable\":false,\"consensusComparisonNote\":\"当前版本没有接入分析师一致预期，因此无法判断 Beat/Miss。\",\"qualityScore\":8,\"summary\":\"Good\"}";
        ObjectNode responseNode = new ObjectMapper().createObjectNode();
        responseNode.put("id", "resp_test").putArray("output").addObject()
                .putArray("content").addObject().put("type", "output_text").put("text", analysis);
        String response = responseNode.toString();
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response));
        EarningsProperties properties = new EarningsProperties();
        properties.getOpenai().setBaseUrl(server.url("/").toString());
        properties.getOpenai().setApiKey("secret-test-key"); properties.getOpenai().setModel("test-model");
        OpenAiClient client = new OpenAiClient();
        ReflectionTestUtils.setField(client, "webClientBuilder", WebClient.builder());
        ReflectionTestUtils.setField(client, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(client, "properties", properties);
        client.initialize();
        assertThat(client.analyze("filing").getTicker()).isEqualTo("CRDO");
        RecordedRequest request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(request.getPath()).isEqualTo("/v1/responses");
        assertThat(requestBody).contains("\"type\":\"json_schema\"", "\"strict\":true", "financial_analysis");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer secret-test-key");
    }
}

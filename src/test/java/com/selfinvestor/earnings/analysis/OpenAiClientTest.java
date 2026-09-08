package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.config.EarningsProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientTest {
    private MockWebServer server;

    @BeforeEach void start() throws Exception { server = new MockWebServer(); server.start(); }
    @AfterEach void stop() throws Exception { server.shutdown(); }

    @Test
    void usesResponsesStructuredOutputAndDeserializesDto() throws Exception {
        String analysis = "{\"company\":\"Credo\",\"ticker\":\"CRDO\",\"fiscalQuarter\":null,\"headline\":\"Strong\",\"financials\":null,\"guidance\":null,\"segments\":[],\"positives\":[],\"negatives\":[],\"risks\":[],\"managementCommentary\":[],\"fundamentalTrend\":\"IMPROVING\",\"fundamentalTrendReason\":\"Growth\",\"investmentThesisChanged\":false,\"investmentThesisChangeReason\":null,\"consensusComparisonAvailable\":false,\"consensusComparisonNote\":\"当前版本没有接入分析师一致预期，因此无法判断 Beat/Miss。\",\"qualityScore\":8,\"summary\":\"Good\"}";
        var responseNode = new ObjectMapper().createObjectNode();
        responseNode.put("id", "resp_test").putArray("output").addObject()
                .putArray("content").addObject().put("type", "output_text").put("text", analysis);
        String response = responseNode.toString();
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response));
        EarningsProperties properties = new EarningsProperties();
        properties.getOpenai().setBaseUrl(server.url("/").toString());
        properties.getOpenai().setApiKey("secret-test-key"); properties.getOpenai().setModel("test-model");
        OpenAiClient client = new OpenAiClient(WebClient.builder(), new ObjectMapper(), properties);
        assertThat(client.analyze("filing").ticker()).isEqualTo("CRDO");
        var request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(request.getPath()).isEqualTo("/v1/responses");
        assertThat(requestBody).contains("\"type\":\"json_schema\"", "\"strict\":true", "financial_analysis");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer secret-test-key");
    }
}

package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.config.DeepSeekProperties;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.config.LlmProperties;
import com.selfinvestor.earnings.llm.strategy.DeepSeekLlmStrategy;
import com.selfinvestor.earnings.llm.strategy.OpenAiLlmStrategy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

class LlmStrategyHttpTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final EarningsProperties earnings = new EarningsProperties();
    private final DeepSeekProperties deepSeek = new DeepSeekProperties();
    private MockWebServer server;
    private JsonNode schema;
    private List<LlmStrategy> strategies;
    private StructuredOutputParser parser;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        earnings.getOpenai().setBaseUrl(server.url("/").toString());
        earnings.getOpenai().setApiKey("openai-test-secret");
        earnings.getOpenai().setModel("openai-test-model");
        deepSeek.setBaseUrl(server.url("/").toString());
        deepSeek.setApiKey("deepseek-test-secret");
        deepSeek.setModel("deepseek-test-model");
        deepSeek.setMaxInputChars(42000);
        schema = mapper.readTree("""
                {"type":"object","additionalProperties":false,"required":["ticker"],
                 "properties":{"ticker":{"type":"string","enum":["CRDO"]}}}
                """);
        var transport = new LlmHttpTransport();
        ReflectionTestUtils.setField(transport, "webClientBuilder", WebClient.builder());
        var openAiStrategy = new OpenAiLlmStrategy();
        ReflectionTestUtils.setField(openAiStrategy, "properties", earnings);
        ReflectionTestUtils.setField(openAiStrategy, "mapper", mapper);
        ReflectionTestUtils.setField(openAiStrategy, "transport", transport);
        var deepSeekStrategy = new DeepSeekLlmStrategy();
        ReflectionTestUtils.setField(deepSeekStrategy, "properties", deepSeek);
        ReflectionTestUtils.setField(deepSeekStrategy, "mapper", mapper);
        ReflectionTestUtils.setField(deepSeekStrategy, "transport", transport);
        strategies = List.of(openAiStrategy, deepSeekStrategy);
        parser = new StructuredOutputParser();
        ReflectionTestUtils.setField(parser, "mapper", mapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void defaultStrategyPreservesOpenAiStrictResponsesContract() throws Exception {
        var response = mapper.createObjectNode().put("status", "completed");
        response.putArray("output").addObject().putArray("content").addObject()
                .put("type", "output_text").put("text", "{\"ticker\":\"CRDO\"}");
        enqueue(response.toString());
        var client = client(new LlmProperties());
        assertThat(generate(client).ticker()).isEqualTo("CRDO");
        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/v1/responses");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer openai-test-secret");
        var body = mapper.readTree(request.getBody().readUtf8());
        assertThat(body.path("model").asText()).isEqualTo("openai-test-model");
        assertThat(body.at("/text/format/type").asText()).isEqualTo("json_schema");
        assertThat(body.at("/text/format/strict").asBoolean()).isTrue();
        assertThat(body.at("/text/format/schema")).isEqualTo(schema);
        assertThat(client.provider()).isEqualTo(LlmProvider.OPENAI);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/v1/"})
    void deepSeekUsesOwnConfigAndFinalContentWithoutOpenAiKey(String basePath) throws Exception {
        earnings.getOpenai().setApiKey(null);
        deepSeek.setBaseUrl(server.url(basePath).toString());
        deepSeek.setThinkingEnabled(true);
        enqueue(deepSeekResponse("{\"ticker\":\"CRDO\"}", "stop"));
        var client = deepSeekClient();
        assertThat(generate(client).ticker()).isEqualTo("CRDO");
        assertThat(client.modelName()).isEqualTo("deepseek-test-model");
        assertThat(client.maxInputChars()).isEqualTo(42000);
        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo(basePath + "chat/completions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer deepseek-test-secret");
        var body = mapper.readTree(request.getBody().readUtf8());
        assertThat(body.path("model").asText()).isEqualTo("deepseek-test-model");
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("max_tokens").asInt()).isEqualTo(8192);
        assertThat(body.at("/thinking/type").asText()).isEqualTo("enabled");
        assertThat(body.at("/response_format/type").asText()).isEqualTo("json_object");
        assertThat(body.has("text")).isFalse();
        assertThat(body.at("/messages/0/content").asText()).contains("test instructions", "JSON", schema.toString());
        assertThat(body.at("/messages/1/content").asText()).isEqualTo("test filing");
        assertThat(server.getRequestCount()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not JSON", "null", "[]", "{}", "{\"ticker\":1}",
            "{\"ticker\":\"UNKNOWN\"}", "{\"ticker\":\"CRDO\",\"extra\":true}",
            "{\"ticker\":\"CRDO\"} {}", "{\"ticker\":\"CRDO\",\"ticker\":\"CRDO\"}"})
    void invalidOutputIsRejectedWithoutHiddenRepairCalls(String content) {
        enqueue(deepSeekResponse(content, "stop"));
        assertThatThrownBy(() -> generate(deepSeekClient())).isInstanceOf(IllegalArgumentException.class);
        assertThat(server.getRequestCount()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"length", "content_filter", "tool_calls", ""})
    void refusesIncompleteOutputEvenIfContentLooksValid(String finishReason) {
        enqueue(deepSeekResponse("{\"ticker\":\"CRDO\"}", finishReason));
        assertThatThrownBy(() -> generate(deepSeekClient())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not finish normally");
        assertThat(server.getRequestCount()).isOne();
    }

    @Test
    void missingSelectedKeyFailsBeforeNetworkAndDoesNotFallback() {
        deepSeek.setApiKey("");
        assertThatThrownBy(() -> generate(deepSeekClient())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEEPSEEK_API_KEY");
        assertThat(server.getRequestCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void permanentHttpErrorsAreSanitizedAndNotRetried(int status) {
        server.enqueue(new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"private payload and deepseek-test-secret\"}"));
        assertThatThrownBy(() -> generate(deepSeekClient())).isInstanceOf(IllegalStateException.class)
                .hasMessage("DEEPSEEK API request failed: HTTP " + status).hasNoCause();
        assertThat(server.getRequestCount()).isOne();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void transientHttpErrorsHaveBoundedRetries(int status) {
        server.enqueue(new MockResponse().setResponseCode(status));
        enqueue(deepSeekResponse("{\"ticker\":\"CRDO\"}", "stop"));
        assertThat(generate(deepSeekClient()).ticker()).isEqualTo("CRDO");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void openAiV1PrefixIsNotDuplicatedAndIncompleteResponsesFail() {
        earnings.getOpenai().setBaseUrl(server.url("/v1/").toString());
        enqueue("{\"status\":\"incomplete\",\"output\":[]}");
        assertThatThrownBy(() -> generate(client(new LlmProperties())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not completed");
        try {
            assertThat(server.takeRequest(2, TimeUnit.SECONDS).getPath()).isEqualTo("/v1/responses");
        } catch (InterruptedException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void duplicateOrMissingStrategyFailsAtStartup() {
        strategies = List.of(strategies.get(0), strategies.get(0));
        assertThatThrownBy(() -> client(new LlmProperties())).hasMessageContaining("Duplicate LLM strategy");
        strategies = List.of();
        assertThatThrownBy(this::deepSeekClient).hasMessageContaining("No LLM strategy");
    }

    @Test
    void remoteSchemaReferencesAreRejectedBeforeNetwork() throws Exception {
        schema = mapper.readTree("{\"$ref\":\"https://example.invalid/schema\"}");
        assertThatThrownBy(() -> generate(deepSeekClient())).hasMessageContaining("local JSON Schema references");
        assertThat(server.getRequestCount()).isZero();
    }

    private Ticker generate(LlmClient client) {
        return client.generateStructured("test filing", "test instructions", schema, "test_schema", Ticker.class);
    }

    private LlmClient deepSeekClient() {
        var config = new LlmProperties();
        config.setProvider(LlmProvider.DEEPSEEK);
        return client(config);
    }

    private LlmClient client(LlmProperties config) {
        var client = new LlmClient();
        ReflectionTestUtils.setField(client, "strategies", strategies);
        ReflectionTestUtils.setField(client, "properties", config);
        ReflectionTestUtils.setField(client, "parser", parser);
        client.initialize();
        return client;
    }

    private String deepSeekResponse(String content, String reason) {
        var response = mapper.createObjectNode();
        var choice = response.putArray("choices").addObject().put("finish_reason", reason);
        choice.putObject("message").put("role", "assistant").put("content", content)
                .put("reasoning_content", "not JSON; this must never be parsed as the answer");
        return response.toString();
    }

    private void enqueue(String body) {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(body));
    }

    record Ticker(String ticker) { }
}

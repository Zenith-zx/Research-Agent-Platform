package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.config.EarningsProperties;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class OpenAiClient {

    @Resource
    private WebClient.Builder webClientBuilder;

    @Resource
    private ObjectMapper mapper;

    @Resource
    private EarningsProperties properties;

    private WebClient client;

    private String systemPrompt;

    private JsonNode schema;

    private String marketStressSystemPrompt;

    private JsonNode marketStressSchema;

    /**
     * 初始化 OpenAI 客户端、系统提示词和 JSON Schema。
     */
    @PostConstruct
    void initialize() {
        EarningsProperties.OpenAi openAi = properties.getOpenai();
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(openAi.getConnectTimeout().toMillis()))
                .responseTimeout(openAi.getReadTimeout());
        String proxyHost = openAi.getProxyHost();
        int proxyPort = openAi.getProxyPort();
        if (proxyHost != null && !proxyHost.isBlank()) {
            if (proxyPort <= 0 || proxyPort > 65535) {
                throw new IllegalStateException("OPENAI_PROXY_PORT must be between 1 and 65535");
            }
            httpClient = httpClient.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort));
            log.info("OpenAI HTTP proxy enabled: host={} port={}", proxyHost, proxyPort);
        }
        client = webClientBuilder.clone()
                .baseUrl(openAi.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        systemPrompt = readResource("prompts/financial-analysis-system.txt");
        try {
            schema = mapper.readTree(readResource("prompts/financial-analysis-schema.json"));
            marketStressSystemPrompt = readResource("prompts/market-stress-system-prompt.txt");
            marketStressSchema = mapper.readTree(readResource("prompts/market-stress-analysis-schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid OpenAI structured output schema", exception);
        }
    }

    /**
     * 调用 OpenAI Responses API 分析财报内容。
     *
     * @param input 财报上下文
     * @return 结构化分析结果
     */
    public FinancialAnalysisDTO analyze(String input) {
        FinancialAnalysisDTO analysis = generateStructured(input, systemPrompt, schema,
                "financial_analysis", FinancialAnalysisDTO.class);
        if (analysis.isConsensusComparisonAvailable()) {
            throw new IllegalArgumentException("Consensus comparison is unavailable in this MVP");
        }
        return analysis;
    }

    /**
     * 调用同一个 Responses API 客户端完成市场压力结构化分析。
     *
     * @param input 市场快照和新闻上下文
     * @return 市场压力分析
     */
    public MarketStressAnalysisDTO analyzeMarketStress(String input) {
        return generateStructured(input, marketStressSystemPrompt, marketStressSchema,
                "market_stress_analysis", MarketStressAnalysisDTO.class);
    }

    /**
     * 使用严格 JSON Schema 调用 Responses API 并反序列化。
     *
     * @param input 用户输入
     * @param instructions 系统提示词
     * @param outputSchema JSON Schema
     * @param schemaName Schema 名称
     * @param resultType 结果类型
     * @param <T> 结果泛型
     * @return 结构化结果
     */
    public <T> T generateStructured(String input, String instructions, JsonNode outputSchema,
                                    String schemaName, Class<T> resultType) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        ObjectNode request = mapper.createObjectNode();
        request.put("model", properties.getOpenai().getModel());
        request.put("instructions", instructions);
        request.put("input", input);
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", outputSchema);
        JsonNode response;
        try {
            response = client.post().uri("/v1/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(8))
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn(
                                    "OpenAI request retry: schema={} attempt={} reason={}",
                                    schemaName, signal.totalRetries() + 1, signal.failure().getMessage()))
                            .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
                    .block();
        } catch (WebClientRequestException exception) {
            throw new IllegalStateException(
                    "OpenAI API connection failed; verify network, DNS and OPENAI_PROXY_HOST/OPENAI_PROXY_PORT",
                    exception);
        } catch (WebClientResponseException exception) {
            throw new IllegalStateException(
                    "OpenAI API request failed: HTTP " + exception.getStatusCode().value(), exception);
        }
        try {
            return mapper.readValue(extractOutputText(response), resultType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("OpenAI returned invalid " + schemaName + " output", exception);
        }
    }

    /**
     * 从 Responses API 响应中提取模型文本。
     *
     * @param response API 响应
     * @return 模型输出文本
     */
    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI returned an empty response");
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IllegalStateException("OpenAI refused the analysis: " + content.path("refusal").asText());
                }
            }
        }
        log.warn("OpenAI response contained no output_text, responseId={}", response.path("id").asText());
        throw new IllegalStateException("OpenAI response contained no output_text");
    }
    /**
     * 判断 OpenAI 请求异常是否允许重试。
     *
     * @param error 请求异常
     * @return 是否允许重试
     */
    private boolean isRetryable(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
        }
        if (error instanceof WebClientRequestException) {
            return true;
        }
        return error instanceof TimeoutException;
    }
    /**
     * 读取类路径文本资源。
     *
     * @param path 资源路径
     * @return 资源文本
     */
    private static String readResource(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read classpath resource: " + path, exception);
        }
    }
}

package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

    /**
     * 初始化 OpenAI 客户端、系统提示词和 JSON Schema。
     */
    @PostConstruct
    void initialize() {
        client = webClientBuilder.clone().baseUrl(properties.getOpenai().getBaseUrl()).build();
        systemPrompt = readResource("prompts/financial-analysis-system.txt");
        try {
            schema = mapper.readTree(readResource("prompts/financial-analysis-schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid financial analysis schema", exception);
        }
    }

    /**
     * 调用 OpenAI Responses API 分析财报内容。
     *
     * @param input 财报上下文
     * @return 结构化分析结果
     */
    public FinancialAnalysisDTO analyze(String input) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        ObjectNode request = mapper.createObjectNode();
        request.put("model", properties.getOpenai().getModel());
        request.put("instructions", systemPrompt);
        request.put("input", input);
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "financial_analysis");
        format.put("strict", true);
        format.set("schema", schema);
        JsonNode response = client.post().uri("/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .retrieve().bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(8)).filter(this::isRetryable))
                .block();
        String output = extractOutputText(response);
        try {
            FinancialAnalysisDTO analysis = mapper.readValue(output, FinancialAnalysisDTO.class);
            if (analysis.isConsensusComparisonAvailable()) {
                throw new IllegalArgumentException("Consensus comparison is unavailable in this MVP");
            }
            return analysis;
        } catch (IOException exception) {
            throw new IllegalArgumentException("OpenAI returned invalid structured analysis", exception);
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
        if (!(error instanceof WebClientResponseException response)) {
            return true;
        }
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
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

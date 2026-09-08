package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.config.EarningsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class OpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final WebClient client;
    private final ObjectMapper mapper;
    private final EarningsProperties properties;
    private final String systemPrompt;
    private final JsonNode schema;

    public OpenAiClient(WebClient.Builder builder, ObjectMapper mapper, EarningsProperties properties) {
        this.mapper = mapper; this.properties = properties;
        this.client = builder.clone().baseUrl(properties.getOpenai().getBaseUrl()).build();
        this.systemPrompt = readResource("prompts/financial-analysis-system.txt");
        try { this.schema = mapper.readTree(readResource("prompts/financial-analysis-schema.json")); }
        catch (IOException exception) { throw new IllegalStateException("Invalid financial analysis schema", exception); }
    }

    public FinancialAnalysis analyze(String input) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is not configured");
        ObjectNode request = mapper.createObjectNode();
        request.put("model", properties.getOpenai().getModel());
        request.put("instructions", systemPrompt);
        request.put("input", input);
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema"); format.put("name", "financial_analysis"); format.put("strict", true); format.set("schema", schema);
        JsonNode response = client.post().uri("/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .retrieve().bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(8)).filter(this::isRetryable))
                .block();
        String output = extractOutputText(response);
        try {
            FinancialAnalysis analysis = mapper.readValue(output, FinancialAnalysis.class);
            if (analysis.consensusComparisonAvailable()) throw new IllegalArgumentException("Consensus comparison is unavailable in this MVP");
            return analysis;
        } catch (IOException exception) {
            throw new IllegalArgumentException("OpenAI returned invalid structured analysis", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) throw new IllegalStateException("OpenAI returned an empty response");
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
                if ("refusal".equals(content.path("type").asText())) throw new IllegalStateException("OpenAI refused the analysis: " + content.path("refusal").asText());
            }
        }
        log.warn("OpenAI response contained no output_text, responseId={}", response.path("id").asText());
        throw new IllegalStateException("OpenAI response contained no output_text");
    }
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof WebClientResponseException response)) return true;
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
    private static String readResource(String path) {
        try { return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new IllegalStateException("Cannot read classpath resource: " + path, exception); }
    }
}

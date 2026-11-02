package com.selfinvestor.earnings.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.llm.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * OpenAI Responses 策略，继续使用 text.format.json_schema strict 协议。
 */
@Component
public class OpenAiLlmStrategy implements LlmStrategy {
    @Resource
    private EarningsProperties properties;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private LlmHttpTransport transport;

    /**
     * 标识 OpenAI 实现。
     */
    @Override
    public LlmProvider provider() {
        return LlmProvider.OPENAI;
    }

    /**
     * 返回原配置中的模型名，兼容 OPENAI_MODEL。
     */
    @Override
    public String modelName() {
        return properties.getOpenai().getModel();
    }

    /**
     * 继续读取原 OPENAI_MAX_INPUT_CHARS 配置。
     */
    @Override
    public int maxInputChars() {
        return properties.getOpenai().getMaxInputChars();
    }

    /**
     * 按 Responses 协议发送结构化请求，拒绝未完成响应和拒答内容。
     */
    @Override
    public String generate(StructuredPrompt prompt) {
        var config = properties.getOpenai();
        var request = mapper.createObjectNode();
        request.put("model", modelName()).put("instructions", prompt.instructions()).put("input", prompt.input());
        var format = request.putObject("text").putObject("format");
        format.put("type", "json_schema").put("name", prompt.schemaName()).put("strict", true);
        format.set("schema", prompt.outputSchema());
        var connection = new LlmHttpTransport.Connection(config.getBaseUrl(), config.getConnectTimeout(),
                config.getReadTimeout(), config.getProxyHost(), config.getProxyPort());
        String path = config.getBaseUrl().replaceAll("/+$", "").endsWith("/v1") ? "/responses" : "/v1/responses";
        JsonNode response = transport.post(provider(), connection, path, config.getApiKey(), request);
        if (response.has("status") && !"completed".equals(response.path("status").asText())) {
            throw new IllegalStateException("OPENAI response was not completed");
        }
        StringBuilder answer = new StringBuilder();
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IllegalStateException("OPENAI refused the analysis");
                }
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    answer.append(content.path("text").textValue());
                }
            }
        }
        if (answer.toString().isBlank()) {
            throw new IllegalArgumentException("OPENAI returned no output_text");
        }
        return answer.toString();
    }
}

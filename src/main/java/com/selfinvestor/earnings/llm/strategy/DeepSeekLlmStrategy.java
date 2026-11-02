package com.selfinvestor.earnings.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.config.DeepSeekProperties;
import com.selfinvestor.earnings.llm.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * DeepSeek Chat Completions 策略；JSON Output 不等于 Schema 保证，由共享客户端再校验。
 */
@Component
public class DeepSeekLlmStrategy implements LlmStrategy {
    @Resource
    private DeepSeekProperties properties;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private LlmHttpTransport transport;

    /**
     * 标识 DeepSeek 实现。
     */
    @Override
    public LlmProvider provider() {
        return LlmProvider.DEEPSEEK;
    }

    /**
     * 返回 DeepSeek 独立配置的模型名称。
     */
    @Override
    public String modelName() {
        return properties.getModel();
    }

    /**
     * 返回 DeepSeek 独立的输入字符预算。
     */
    @Override
    public int maxInputChars() {
        return properties.getMaxInputChars();
    }

    /**
     * 使用 json_object 和明确的 Schema 指令，只读取最终 content，不回传思考文本。
     */
    @Override
    public String generate(StructuredPrompt prompt) {
        if (properties.getMaxTokens() < 1) {
            throw new IllegalArgumentException("DEEPSEEK_MAX_TOKENS must be positive");
        }
        var request = mapper.createObjectNode();
        request.put("model", modelName()).put("stream", false).put("max_tokens", properties.getMaxTokens());
        request.putObject("response_format").put("type", "json_object");
        request.putObject("thinking").put("type", properties.isThinkingEnabled() ? "enabled" : "disabled");
        var messages = request.putArray("messages");
        messages.addObject().put("role", "system").put("content", prompt.instructions()
                + "\nReturn exactly one JSON object, no markdown. Follow this JSON Schema (including required, "
                + "additionalProperties, enum, nullability and ranges). Never invent missing evidence.\n"
                + prompt.outputSchema().toString());
        messages.addObject().put("role", "user").put("content", prompt.input());
        var connection = new LlmHttpTransport.Connection(properties.getBaseUrl(), properties.getConnectTimeout(),
                properties.getReadTimeout(), properties.getProxyHost(), properties.getProxyPort());
        JsonNode response = transport.post(provider(), connection, "/chat/completions", properties.getApiKey(), request);
        JsonNode choice = response.path("choices").path(0);
        if (!"stop".equals(choice.path("finish_reason").asText())) {
            throw new IllegalStateException("DEEPSEEK response did not finish normally (truncated, filtered or unexpected)");
        }
        JsonNode content = choice.path("message").path("content");
        if (!content.isTextual() || content.textValue().isBlank()) {
            throw new IllegalArgumentException("DEEPSEEK returned blank final content");
        }
        return content.textValue();
    }
}

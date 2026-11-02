package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 策略模式的上下文：注册策略并按配置分发；业务层不包含 OpenAI/DeepSeek 分支。
 */
@Component
public class LlmClient {
    @Resource
    private List<LlmStrategy> strategies;
    @Resource
    private LlmProperties properties;
    @Resource
    private StructuredOutputParser parser;
    private final Map<LlmProvider, LlmStrategy> registry = new EnumMap<>(LlmProvider.class);
    private LlmStrategy selected;

    /**
     * 启动时固定选择策略，拒绝重复注册与未实现供应商；切换配置需要重启。
     */
    @PostConstruct
    void initialize() {
        for (LlmStrategy strategy : strategies) {
            if (registry.putIfAbsent(strategy.provider(), strategy) != null) {
                throw new IllegalStateException("Duplicate LLM strategy: " + strategy.provider());
            }
        }
        selected = registry.get(properties.getProvider());
        if (selected == null) {
            throw new IllegalStateException("No LLM strategy for " + properties.getProvider());
        }
        if (selected.maxInputChars() < 1) {
            throw new IllegalStateException("LLM maxInputChars must be positive");
        }
    }

    /**
     * 使用选中策略生成并校验结构化数据，不隐式切换供应商、不额外发起内容修复调用。
     */
    public <T> T generateStructured(String input, String instructions, JsonNode outputSchema,
                                    String schemaName, Class<T> resultType) {
        if (input == null || instructions == null || schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("LLM input, instructions and schemaName are required");
        }
        if (modelName() == null || modelName().isBlank()) {
            throw new IllegalStateException(provider() + " model is not configured");
        }
        var schema = parser.prepare(outputSchema);
        String text = selected.generate(new StructuredPrompt(input, instructions, outputSchema, schemaName));
        return parser.parse(text, schema, schemaName, resultType);
    }

    /**
     * 返回当前供应商，供日志与运行诊断使用。
     */
    public LlmProvider provider() {
        return selected.provider();
    }

    /**
     * 返回实际调用的模型名，供分析记录落库。
     */
    public String modelName() {
        return selected.modelName();
    }

    /**
     * 返回当前供应商的输入字符预算，供分块和 Agent Context 使用。
     */
    public int maxInputChars() {
        return selected.maxInputChars();
    }
}

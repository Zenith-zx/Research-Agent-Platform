package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 加载动态 Agent 的系统提示词和严格决策 Schema。
 */
@Component
public class AgentDecisionPromptService {
    @Resource
    private ObjectMapper mapper;

    private String systemPrompt;
    private JsonNode schema;

    /**
     * 从类路径加载不可变 Prompt 资源。
     */
    @PostConstruct
    void initialize() {
        systemPrompt = read("prompts/agent-runtime-system.txt");
        try {
            schema = mapper.readTree(read("prompts/agent-decision-schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid agent decision schema", exception);
        }
    }

    /**
     * 返回 Agent 系统提示词。
     *
     * @return 系统提示词
     */
    public String systemPrompt() {
        return systemPrompt;
    }

    /**
     * 返回决策 JSON Schema。
     *
     * @return JSON Schema
     */
    public JsonNode schema() {
        return schema;
    }

    /**
     * 读取 UTF-8 类路径资源。
     *
     * @param path 资源路径
     * @return 资源内容
     */
    private String read(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read agent prompt resource: " + path, exception);
        }
    }
}

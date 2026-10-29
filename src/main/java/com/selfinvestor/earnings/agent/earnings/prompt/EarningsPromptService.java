package com.selfinvestor.earnings.agent.earnings.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Earnings Agent Prompt 和 Structured Output Schema 的唯一加载入口。
 */
@Component
public class EarningsPromptService {
    @Resource
    private ObjectMapper mapper;

    private final Map<AgentNodeName, String> prompts = new EnumMap<>(AgentNodeName.class);
    private final Map<AgentNodeName, JsonNode> schemas = new EnumMap<>(AgentNodeName.class);

    /**
     * 在应用启动时加载五个节点的 Prompt 和 JSON Schema。
     */
    @PostConstruct
    void initialize() {
        load(AgentNodeName.PLANNER, "planner");
        load(AgentNodeName.RESEARCH, "researcher");
        load(AgentNodeName.CRITIC, "critic");
        load(AgentNodeName.REPLANNER, "replanner");
        load(AgentNodeName.FINALIZER, "finalizer");
    }

    /**
     * 获取指定节点的系统 Prompt。
     *
     * @param node 节点名称
     * @return 系统 Prompt
     */
    public String prompt(AgentNodeName node) {
        return required(prompts.get(node), node, "prompt");
    }

    /**
     * 获取指定节点的 Structured Output JSON Schema。
     *
     * @param node 节点名称
     * @return JSON Schema
     */
    public JsonNode schema(AgentNodeName node) {
        JsonNode schema = schemas.get(node);
        if (schema == null) {
            throw new IllegalArgumentException("No schema for node " + node);
        }
        return schema;
    }

    /**
     * 从 classpath 加载同名 Prompt 和 Schema。
     *
     * @param node 节点名称
     * @param name 资源基础名称
     */
    private void load(AgentNodeName node, String name) {
        prompts.put(node, read("prompts/earnings/" + name + ".md"));
        try {
            schemas.put(node, mapper.readTree(read("prompts/earnings/" + name + "-schema.json")));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid earnings agent schema: " + name, exception);
        }
    }

    /**
     * 读取 UTF-8 classpath 文本资源。
     *
     * @param path classpath 相对路径
     * @return 资源文本
     */
    private static String read(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read classpath resource: " + path, exception);
        }
    }

    /**
     * 校验 Prompt 文本存在并返回该文本。
     *
     * @param value Prompt 文本
     * @param node 节点名称
     * @param type 资源类型
     * @return 非空 Prompt 文本
     */
    private static String required(String value, AgentNodeName node, String type) {
        if (value == null) {
            throw new IllegalArgumentException("No " + type + " for node " + node);
        }
        return value;
    }
}

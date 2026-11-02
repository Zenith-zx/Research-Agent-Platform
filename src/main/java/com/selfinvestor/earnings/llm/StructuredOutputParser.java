package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一执行 JSON 语法、Schema 和 DTO 校验，避免 JSON Mode 的静默缺字段或类型强转。
 */
@Component
public class StructuredOutputParser {
    @Resource
    private ObjectMapper mapper;
    private final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final ConcurrentHashMap<String, Schema> schemas = new ConcurrentHashMap<>();

    /**
     * 在调用供应商前编译内部 Schema；只允许本地片段引用，禁止隐式远程加载。
     */
    public Schema prepare(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            throw new IllegalArgumentException("A JSON object Schema is required");
        }
        requireLocalReferences(schema);
        return schemas.computeIfAbsent(schema.toString(), json -> registry.getSchema(json, InputFormat.JSON));
    }

    /**
     * 拒绝空回答、多个 JSON 值、重复键、缺失字段、非法枚举以及 Schema 不符合的结果。
     */
    public <T> T parse(String text, Schema schema, String name, Class<T> resultType) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty structured output: " + name);
        }
        try {
            JsonNode value = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(text);
            if (value == null || !value.isObject()) {
                throw new IllegalArgumentException("Structured output must be a JSON object: " + name);
            }
            if (!schema.validate(value.toString(), InputFormat.JSON).isEmpty()) {
                throw new IllegalArgumentException("Structured output violates JSON Schema: " + name);
            }
            return mapper.readerFor(resultType).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid structured output: " + name);
        }
    }

    /**
     * Schema 来自项目资源；额外拒绝远程引用和自定义远程方言，避免未来误用造成网络访问。
     */
    private void requireLocalReferences(JsonNode node) {
        if (node.isObject()) {
            for (String keyword : java.util.List.of("$ref", "$dynamicRef", "$recursiveRef")) {
                if (node.has(keyword) && !node.path(keyword).asText().startsWith("#")) {
                    throw new IllegalArgumentException("Only local JSON Schema references are allowed");
                }
            }
            if (node.has("$schema") && !"https://json-schema.org/draft/2020-12/schema".equals(node.path("$schema").asText())) {
                throw new IllegalArgumentException("Only bundled Draft 2020-12 is supported");
            }
        }
        node.elements().forEachRemaining(this::requireLocalReferences);
    }
}

package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 与供应商协议无关的单次结构化请求，Schema 来自项目资源而不是模型。
 *
 * @param input 业务上下文
 * @param instructions 系统约束
 * @param outputSchema 预期 JSON Schema
 * @param schemaName 日志和协议使用的结构名称
 */
public record StructuredPrompt(String input, String instructions, JsonNode outputSchema, String schemaName) {
}

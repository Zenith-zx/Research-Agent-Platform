package com.selfinvestor.earnings.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 大模型供应商策略标识，不将模型名称当成供应商分支条件。
 */
@Getter
@AllArgsConstructor
public enum LlmProvider {
    OPENAI("OpenAI Responses API，服务端严格 JSON Schema 输出"),
    DEEPSEEK("DeepSeek Chat Completions API，JSON Output 加本地 Schema 校验");

    /**
     * 供应商接入方式的中文说明。
     */
    private final String desc;
}

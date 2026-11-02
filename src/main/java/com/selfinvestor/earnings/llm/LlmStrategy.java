package com.selfinvestor.earnings.llm;

/**
 * 大模型接入策略；业务层只依赖 LlmClient，不感知具体 HTTP 协议。
 */
public interface LlmStrategy {
    /**
     * 返回该实现负责的供应商，注册时必须唯一。
     */
    LlmProvider provider();

    /**
     * 返回实际配置的模型名称，用于结果审计，不能始终记录 OpenAI 模型。
     */
    String modelName();

    /**
     * 返回当前模型配置的输入字符预算。
     */
    int maxInputChars();

    /**
     * 执行一次生成并返回最终回答 JSON 文本；不得返回供应商的 reasoning_content。
     */
    String generate(StructuredPrompt prompt);
}

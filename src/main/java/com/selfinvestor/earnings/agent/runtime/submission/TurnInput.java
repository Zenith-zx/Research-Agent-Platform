package com.selfinvestor.earnings.agent.runtime.submission;

/**
 * 可转换为 Agent 上下文文本的 Turn 输入。
 */
public interface TurnInput {
    /**
     * 返回供模型读取的输入文本。
     *
     * @return 输入文本
     */
    String asText();
}

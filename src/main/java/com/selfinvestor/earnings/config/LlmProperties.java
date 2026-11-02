package com.selfinvestor.earnings.config;

import com.selfinvestor.earnings.llm.LlmProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 共享大模型策略选择，默认兼容原有 OpenAI 配置。
 */
@Data
@ConfigurationProperties(prefix = "self-investor.llm")
public class LlmProperties {
    private LlmProvider provider = LlmProvider.OPENAI;
}

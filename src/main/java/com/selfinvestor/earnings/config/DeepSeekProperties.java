package com.selfinvestor.earnings.config;

import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 独立连接配置，不复用 OpenAI 密钥、代理或模型名称。
 */
@Data
@ConfigurationProperties(prefix = "self-investor.deepseek")
public class DeepSeekProperties {
    private String baseUrl = "https://api.deepseek.com";
    @ToString.Exclude
    private String apiKey;
    private String model = "deepseek-flash";
    private int maxInputChars = 60000;
    private int maxTokens = 8192;
    private boolean thinkingEnabled;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(120);
    private String proxyHost;
    private int proxyPort;
}

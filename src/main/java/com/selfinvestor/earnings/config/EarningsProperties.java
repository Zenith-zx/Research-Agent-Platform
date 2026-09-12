package com.selfinvestor.earnings.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 应用业务配置。
 */
@Data
@ConfigurationProperties(prefix = "earnings")
public class EarningsProperties {
    /**
     * 监听任务配置。
     */
    private final Monitor monitor = new Monitor();
    /**
     * SEC 接口配置。
     */
    private final Sec sec = new Sec();
    /**
     * OpenAI 接口配置。
     */
    private final OpenAi openai = new OpenAi();
    /**
     * Telegram 通知配置。
     */
    private final Telegram telegram = new Telegram();

    /**
     * Filing 监听配置。
     */
    @Data
    public static class Monitor {
        /**
         * 是否启用监听。
         */
        private boolean enabled = true;
        /**
         * 监听间隔秒数。
         */
        private long intervalSeconds = 30;
        /**
         * 注册 SEC Filing 时允许回溯的自然日数。
         */
        private int filingLookbackDays = 30;
        /**
         * 应用启动时最多恢复的未完成 Filing 数量。
         */
        private int recoveryBatchSize = 20;
    }

    /**
     * SEC HTTP 接口配置。
     */
    @Data
    public static class Sec {
        /**
         * SEC 网站根地址。
         */
        private String baseUrl;
        /**
         * SEC Data API 根地址。
         */
        private String dataBaseUrl;
        /**
         * SEC 要求的 User-Agent。
         */
        private String userAgent;
        /**
         * 连接超时时间。
         */
        private Duration connectTimeout = Duration.ofSeconds(5);
        /**
         * 响应读取超时时间。
         */
        private Duration readTimeout = Duration.ofSeconds(30);
        /**
         * SEC 请求最小时间间隔。
         */
        private Duration minRequestInterval = Duration.ofMillis(120);
    }

    /**
     * OpenAI 接口配置。
     */
    @Data
    public static class OpenAi {
        /**
         * OpenAI API 根地址。
         */
        private String baseUrl;
        /**
         * OpenAI API Key。
         */
        private String apiKey;
        /**
         * 分析使用的模型名称。
         */
        private String model;
        /**
         * 单次输入最大字符数。
         */
        private int maxInputChars = 60000;
        /**
         * OpenAI API 连接超时时间。
         */
        private Duration connectTimeout = Duration.ofSeconds(10);
        /**
         * OpenAI API 响应读取超时时间。
         */
        private Duration readTimeout = Duration.ofSeconds(120);
        /**
         * 可选 HTTP 代理主机；为空时直接连接。
         */
        private String proxyHost;
        /**
         * 可选 HTTP 代理端口。
         */
        private int proxyPort;
    }

    /**
     * Telegram 通知配置。
     */
    @Data
    public static class Telegram {
        /**
         * 是否启用 Telegram 通知。
         */
        private boolean enabled;
        /**
         * Telegram Bot Token。
         */
        private String botToken;
        /**
         * Telegram Chat ID。
         */
        private String chatId;
    }
}

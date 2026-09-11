package com.selfinvestor.earnings.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 市场压力分析配置。
 */
@Data
@ConfigurationProperties(prefix = "market-stress")
public class MarketStressProperties {
    /**
     * 是否启用市场压力模块。
     */
    private boolean enabled;
    /**
     * 需要扫描的标的。
     */
    private List<String> symbols = new ArrayList<>(Collections.singletonList("QQQ"));
    /**
     * 短周期 RSI 周期。
     */
    private int rsiPeriod = 6;
    /**
     * RSI 触发阈值。
     */
    private BigDecimal rsiTrigger = new BigDecimal("30");
    /**
     * 两次分析之间的冷却小时数。
     */
    private long analysisCooldownHours = 12;
    /**
     * 行情和新闻供应商配置。
     */
    private final Provider provider = new Provider();
    /**
     * 调度器配置。
     */
    private final Scheduler scheduler = new Scheduler();
    /**
     * 均值回归评分权重。
     */
    private final Scoring scoring = new Scoring();
    /**
     * 邮件通知配置。
     */
    private final Email email = new Email();

    /**
     * 外部数据供应商配置。
     */
    @Data
    public static class Provider {
        /**
         * Finnhub API 根地址。
         */
        private String baseUrl = "https://finnhub.io/api/v1";
        /**
         * Finnhub API Key。
         */
        private String apiKey;
        /**
         * HTTP 连接超时。
         */
        private Duration connectTimeout = Duration.ofSeconds(5);
        /**
         * HTTP 读取超时。
         */
        private Duration readTimeout = Duration.ofSeconds(30);
        /**
         * 新闻回溯小时数。
         */
        private int newsLookbackHours = 72;
        /**
         * 输入模型的最大新闻数量。
         */
        private int maxNewsArticles = 30;
        /**
         * Finnhub 中用于查询 VIX 的代码。
         */
        private String vixSymbol = "^VIX";
    }

    /**
     * 市场扫描调度配置。
     */
    @Data
    public static class Scheduler {
        /**
         * 是否启用定时扫描。
         */
        private boolean enabled = true;
        /**
         * 扫描间隔分钟数。
         */
        private long intervalMinutes = 60;
        /**
         * 是否仅在美股常规交易时间扫描。
         */
        private boolean marketHoursOnly = true;
    }

    /**
     * 均值回归总分配置。
     */
    @Data
    public static class Scoring {
        /**
         * 技术超卖权重。
         */
        private int technicalOversoldWeight = 20;
        /**
         * 事件可逆性权重。
         */
        private int eventReversibilityWeight = 20;
        /**
         * 基本面损害权重。
         */
        private int fundamentalDamageWeight = 15;
        /**
         * 金融压力权重。
         */
        private int financialStressWeight = 15;
        /**
         * 坏消息定价权重。
         */
        private int badNewsPricingWeight = 20;
        /**
         * 卖方衰竭权重。
         */
        private int sellerExhaustionWeight = 10;
    }

    /**
     * 市场压力邮件配置。
     */
    @Data
    public static class Email {
        /**
         * 是否启用邮件通知。
         */
        private boolean enabled;
        /**
         * 邮件接收地址列表。
         */
        private List<String> recipients = new ArrayList<>();
    }
}

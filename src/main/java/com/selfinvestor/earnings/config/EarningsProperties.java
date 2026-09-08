package com.selfinvestor.earnings.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "earnings")
public class EarningsProperties {
    private final Monitor monitor = new Monitor();
    private final Sec sec = new Sec();
    private final OpenAi openai = new OpenAi();
    private final Telegram telegram = new Telegram();
    public Monitor getMonitor() { return monitor; }
    public Sec getSec() { return sec; }
    public OpenAi getOpenai() { return openai; }
    public Telegram getTelegram() { return telegram; }

    public static class Monitor {
        private boolean enabled = true;
        private long intervalSeconds = 30;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(long intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    }
    public static class Sec {
        private String baseUrl;
        private String dataBaseUrl;
        private String userAgent;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration minRequestInterval = Duration.ofMillis(120);
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getDataBaseUrl() { return dataBaseUrl; }
        public void setDataBaseUrl(String dataBaseUrl) { this.dataBaseUrl = dataBaseUrl; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
        public Duration getMinRequestInterval() { return minRequestInterval; }
        public void setMinRequestInterval(Duration minRequestInterval) { this.minRequestInterval = minRequestInterval; }
    }
    public static class OpenAi {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int maxInputChars = 60000;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxInputChars() { return maxInputChars; }
        public void setMaxInputChars(int maxInputChars) { this.maxInputChars = maxInputChars; }
    }
    public static class Telegram {
        private boolean enabled;
        private String botToken;
        private String chatId;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBotToken() { return botToken; }
        public void setBotToken(String botToken) { this.botToken = botToken; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
    }
}

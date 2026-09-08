package com.selfinvestor.earnings.notification;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.Filing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TelegramNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private final WebClient client;
    private final EarningsProperties.Telegram config;

    public TelegramNotificationService(WebClient.Builder builder, EarningsProperties properties) {
        this.client = builder.clone().baseUrl("https://api.telegram.org").build();
        this.config = properties.getTelegram();
    }

    @Override
    public void send(Filing filing, FinancialAnalysis analysis) {
        if (!config.isEnabled()) return;
        if (config.getBotToken() == null || config.getBotToken().isBlank() || config.getChatId() == null || config.getChatId().isBlank()) {
            throw new IllegalStateException("Telegram is enabled but TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID is missing");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", config.getChatId());
        body.put("disable_web_page_preview", true);
        body.put("text", format(filing, analysis));
        client.post().uri("/bot{token}/sendMessage", config.getBotToken())
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(8)).filter(this::isRetryable)).block();
        log.info("Telegram notification sent: {} accession={}", filing.getCompany().getTicker(), filing.getAccessionNumber());
    }

    private String format(Filing filing, FinancialAnalysis analysis) {
        StringBuilder message = new StringBuilder();
        message.append("📊 ").append(filing.getCompany().getTicker()).append(" 财报分析\n")
                .append(filing.getFormType()).append(" · ").append(filing.getFilingDate()).append("\n\n")
                .append(nullSafe(analysis.headline())).append("\n\n")
                .append("基本面趋势：").append(analysis.fundamentalTrend()).append("\n")
                .append(nullSafe(analysis.fundamentalTrendReason())).append("\n\n")
                .append("质量评分：").append(analysis.qualityScore() == null ? "N/A" : analysis.qualityScore() + "/10").append("\n\n")
                .append(nullSafe(analysis.summary())).append("\n\n")
                .append(analysis.consensusComparisonNote()).append("\n\n")
                .append(filing.getSecUrl());
        return message.length() <= 4000 ? message.toString() : message.substring(0, 3950) + "\n…\n" + filing.getSecUrl();
    }
    private static String nullSafe(String value) { return value == null ? "" : value; }
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof WebClientResponseException response)) return true;
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

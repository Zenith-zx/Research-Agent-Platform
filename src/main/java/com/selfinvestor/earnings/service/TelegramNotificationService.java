package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.service.interfaces.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationService implements NotificationService {
    /**
     * 发送宏观报告摘要并提供 Turn 标识，完整报告保留在查询接口中。
     */
    @Override
    public void sendMacro(String turnId, String report) {
        if (!config.isEnabled()) {
            return;
        }
        if (config.getBotToken() == null || config.getBotToken().isBlank()
                || config.getChatId() == null || config.getChatId().isBlank()) {
            throw new IllegalStateException("Telegram credentials are missing");
        }
        String text = "US_MACRO 研究（非交易指令）\nTurn: " + turnId + "\n\n"
                + report.substring(0, Math.min(3500, report.length()))
                + "\n\n完整结果：GET /api/macro/turns/" + turnId + "/analysis";
        client.post().uri("/bot{token}/sendMessage", config.getBotToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("chat_id", config.getChatId(), "text", text, "disable_web_page_preview", true))
                .retrieve().toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(8))
                        .filter(this::isRetryable)).block();
    }
    @Resource
    private WebClient.Builder webClientBuilder;
    @Resource
    private EarningsProperties properties;
    private WebClient client;
    private EarningsProperties.Telegram config;

    /**
     * 初始化 Telegram HTTP 客户端和通知配置。
     */
    @PostConstruct
    public void initialize() {
        client = webClientBuilder.clone().baseUrl("https://api.telegram.org").build();
        config = properties.getTelegram();
    }

    /**
     * 向 Telegram 发送财报分析摘要。
     *
     * @param filing Filing 实体
     * @param analysis 结构化分析结果
     */
    @Override
    public void send(FilingPO filing, FinancialAnalysisDTO analysis) {
        if (!config.isEnabled()) {
            return;
        }
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

    /**
     * 格式化 Telegram 通知文本。
     *
     * @param filing Filing 实体
     * @param analysis 结构化分析结果
     * @return 通知文本
     */
    private String format(FilingPO filing, FinancialAnalysisDTO analysis) {
        StringBuilder message = new StringBuilder();
        message.append("📊 ").append(filing.getCompany().getTicker()).append(" 财报分析\n")
                .append(filing.getFormType()).append(" · ").append(filing.getFilingDate()).append("\n\n")
                .append(nullSafe(analysis.getHeadline())).append("\n\n")
                .append("基本面趋势：").append(analysis.getFundamentalTrend()).append("\n")
                .append(nullSafe(analysis.getFundamentalTrendReason())).append("\n\n")
                .append("质量评分：").append(analysis.getQualityScore() == null ? "N/A" : analysis.getQualityScore() + "/10").append("\n\n")
                .append(nullSafe(analysis.getSummary())).append("\n\n")
                .append(analysis.getConsensusComparisonNote()).append("\n\n")
                .append(filing.getSecUrl());
        return message.length() <= 4000 ? message.toString() : message.substring(0, 3950) + "\n…\n" + filing.getSecUrl();
    }
    /**
     * 将空值转换为空字符串。
     *
     * @param value 原始字符串
     * @return 非空字符串
     */
    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 判断 Telegram 请求异常是否允许重试。
     *
     * @param error 请求异常
     * @return 是否允许重试
     */
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof WebClientResponseException response)) {
            return true;
        }
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

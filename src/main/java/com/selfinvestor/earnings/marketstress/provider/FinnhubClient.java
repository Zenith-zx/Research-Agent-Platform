package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.MarketStressProperties;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.MapUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * Finnhub 行情与新闻共用 HTTP 客户端。
 */
@Component
public class FinnhubClient {
    private static final String API_KEY_HEADER = "X-Finnhub-Token";

    @Resource
    private WebClient.Builder webClientBuilder;
    @Resource
    private MarketStressProperties properties;

    private WebClient client;

    /**
     * 初始化 Finnhub WebClient。
     */
    @PostConstruct
    public void initialize() {
        MarketStressProperties.Provider config = properties.getProvider();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(config.getConnectTimeout().toMillis()))
                .responseTimeout(config.getReadTimeout());
        client = webClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 调用 Finnhub GET 接口，API Key 仅放入请求头。
     *
     * @param path API 路径
     * @param parameters 查询参数
     * @return JSON 响应
     */
    public JsonNode get(String path, Map<String, ?> parameters) {
        String apiKey = properties.getProvider().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FINNHUB_API_KEY is not configured");
        }
        return client.get().uri(builder -> {
            builder.path(path);
            if (MapUtils.isNotEmpty(parameters)) {
                for (Map.Entry<String, ?> parameter : parameters.entrySet()) {
                    builder.queryParam(parameter.getKey(), parameter.getValue());
                }
            }
            return builder.build();
        }).header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(this::isRetryable))
                .block();
    }

    /**
     * 判断供应商请求异常是否可重试。
     *
     * @param error 请求异常
     * @return 是否可重试
     */
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof WebClientResponseException response)) {
            return true;
        }
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

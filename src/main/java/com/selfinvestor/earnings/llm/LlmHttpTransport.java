package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * 复用连接、超时和有限瞬态重试；不负责路由、不跨供应商降级，也不记录密钥/原始响应。
 */
@Component
public class LlmHttpTransport {
    @Resource
    private WebClient.Builder webClientBuilder;

    /**
     * 连接设置不包含 API Key，避免默认 toString 意外泄露凭据。
     */
    public record Connection(String baseUrl, Duration connectTimeout, Duration readTimeout,
                             String proxyHost, int proxyPort) {
    }

    /**
     * 发送 JSON 请求，只在 429、5xx、网络故障时最多重试三次。
     * 仅实际调用时建立客户端和校验连接，未选中的供应商不需要配置密钥。
     */
    public JsonNode post(LlmProvider provider, Connection connection, String path, String apiKey, JsonNode request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(provider + "_API_KEY is not configured");
        }
        WebClient client = client(connection);
        try {
            JsonNode response = client.post().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(request).retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(8))
                            .filter(this::isRetryable)
                            .onRetryExhaustedThrow((retry, signal) -> signal.failure()))
                    .block(connection.readTimeout().multipliedBy(4).plusSeconds(30));
            if (response == null) {
                throw new IllegalStateException(provider + " returned an empty HTTP response");
            }
            return response;
        } catch (WebClientResponseException exception) {
            throw new IllegalStateException(provider + " API request failed: HTTP " + exception.getStatusCode().value());
        } catch (WebClientRequestException exception) {
            throw new IllegalStateException(provider + " API connection failed; check network, endpoint and proxy");
        }
    }

    /**
     * 构建可复用底层连接池的 WebClient；保留配置的路径前缀并限制响应大小。
     */
    private WebClient client(Connection connection) {
        URI uri = URI.create(connection.baseUrl());
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("LLM base URL must be an HTTP(S) endpoint without credentials/query/fragment");
        }
        if (connection.connectTimeout() == null || connection.connectTimeout().isNegative()
                || connection.connectTimeout().isZero() || connection.readTimeout() == null
                || connection.readTimeout().isNegative() || connection.readTimeout().isZero()) {
            throw new IllegalArgumentException("LLM timeouts must be positive");
        }
        HttpClient http = HttpClient.create().compress(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connection.connectTimeout().toMillis()))
                .responseTimeout(connection.readTimeout());
        if (connection.proxyHost() != null && !connection.proxyHost().isBlank()) {
            if (connection.proxyPort() < 1 || connection.proxyPort() > 65535) {
                throw new IllegalArgumentException("LLM proxy port must be between 1 and 65535");
            }
            http = http.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(connection.proxyHost()).port(connection.proxyPort()));
        }
        String baseUrl = connection.baseUrl().replaceAll("/+$", "");
        return webClientBuilder.clone().baseUrl(baseUrl)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(http)).build();
    }

    /**
     * 只重试可恢复的传输故障，鉴权、参数错误与非法模型输出不自动重试。
     */
    private boolean isRetryable(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
        }
        return error instanceof WebClientRequestException || error instanceof TimeoutException;
    }
}

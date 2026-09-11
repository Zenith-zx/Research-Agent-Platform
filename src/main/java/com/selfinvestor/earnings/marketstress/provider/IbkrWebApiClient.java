package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.MarketStressProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * IBKR Client Portal Gateway HTTP 客户端。
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "market-stress.provider", name = "market-data", havingValue = "ibkr")
public class IbkrWebApiClient {
    private static final Duration SESSION_STATUS_CACHE = Duration.ofMinutes(3);

    @Resource
    private WebClient.Builder webClientBuilder;
    @Resource
    private MarketStressProperties properties;

    private WebClient client;
    private Instant lastAuthenticatedAt;

    /**
     * 创建连接本地 Client Portal Gateway 的 HTTP 客户端。
     */
    @PostConstruct
    public void initialize() {
        MarketStressProperties.Ibkr config = properties.getProvider().getIbkr();
        URI baseUri = URI.create(config.getBaseUrl());
        validateTlsConfiguration(baseUri, config.isVerifySsl());
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(config.getConnectTimeout().toMillis()))
                .responseTimeout(config.getReadTimeout());
        if (!config.isVerifySsl()) {
            httpClient = configureInsecureLocalTls(httpClient);
        }
        client = webClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 确保 Brokerage Session 可用后执行 GET 请求。
     *
     * @param path API 路径
     * @param parameters 查询参数
     * @return JSON 响应
     */
    public JsonNode get(String path, Map<String, ?> parameters) {
        ensureBrokerageSession();
        return executeGet(path, parameters);
    }

    /**
     * 保持 Client Portal Brokerage Session 活跃。
     */
    public void keepAlive() {
        ensureBrokerageSession();
        executeGet("/tickle", Collections.emptyMap());
    }

    /**
     * 检查会话，必要时尝试重新初始化 Brokerage Session。
     */
    public synchronized void ensureBrokerageSession() {
        Instant now = Instant.now();
        if (lastAuthenticatedAt != null
                && Duration.between(lastAuthenticatedAt, now).compareTo(SESSION_STATUS_CACHE) < 0) {
            return;
        }
        JsonNode status = executePost("/iserver/auth/status", null);
        JsonNode statusValue = unwrapStatus(status);
        if (statusValue.path("authenticated").asBoolean(false)
                && statusValue.path("connected").asBoolean(false)) {
            lastAuthenticatedAt = now;
            return;
        }
        if (!statusValue.path("connected").asBoolean(false)) {
            throw new IllegalStateException("IBKR Client Portal Gateway is not connected; log in through the Gateway first");
        }
        Map<String, Boolean> body = Map.of(
                "publish", Boolean.TRUE,
                "compete", properties.getProvider().getIbkr().isCompeteSession());
        JsonNode initialized = unwrapStatus(executePost("/iserver/auth/ssodh/init", body));
        if (!initialized.path("authenticated").asBoolean(false)) {
            throw new IllegalStateException("IBKR brokerage session is not authenticated: "
                    + initialized.path("message").asText("manual login required"));
        }
        lastAuthenticatedAt = now;
        log.info("IBKR brokerage session initialized");
    }

    /**
     * 执行无需再次校验会话的 GET 请求。
     *
     * @param path API 路径
     * @param parameters 查询参数
     * @return JSON 响应
     */
    private JsonNode executeGet(String path, Map<String, ?> parameters) {
        return client.get().uri(builder -> {
            builder.path(path);
            if (MapUtils.isNotEmpty(parameters)) {
                for (Map.Entry<String, ?> parameter : parameters.entrySet()) {
                    builder.queryParam(parameter.getKey(), parameter.getValue());
                }
            }
            return builder.build();
        }).retrieve().bodyToMono(JsonNode.class)
                .retryWhen(retrySpec())
                .block();
    }

    /**
     * 执行无需再次校验会话的 POST 请求。
     *
     * @param path API 路径
     * @param body 请求体，允许为空
     * @return JSON 响应
     */
    private JsonNode executePost(String path, Object body) {
        WebClient.RequestBodySpec request = client.post().uri(path);
        if (body != null) {
            request.bodyValue(body);
        }
        return request.retrieve().bodyToMono(JsonNode.class)
                .retryWhen(retrySpec())
                .block();
    }

    /**
     * 创建 IBKR 网络错误退避规则。
     *
     * @return 重试规则
     */
    private Retry retrySpec() {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(8))
                .filter(this::isRetryable);
    }

    /**
     * 判断异常是否适合重试。
     *
     * @param error 请求异常
     * @return 是否重试
     */
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof WebClientResponseException response)) {
            return true;
        }
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }

    /**
     * 兼容 IBKR 会话状态的扁平与 success.value 两种响应结构。
     *
     * @param response 原始响应
     * @return 会话状态节点
     */
    private JsonNode unwrapStatus(JsonNode response) {
        JsonNode nested = response.path("success").path("value");
        if (nested.isObject()) {
            return nested;
        }
        return response;
    }

    /**
     * 为本机自签名 Client Portal Gateway 配置 TLS。
     *
     * @param httpClient Reactor Netty 客户端
     * @return 配置后的客户端
     */
    private HttpClient configureInsecureLocalTls(HttpClient httpClient) {
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            return httpClient.secure(spec -> spec.sslContext(sslContext));
        } catch (SSLException exception) {
            throw new IllegalStateException("Cannot configure IBKR Gateway TLS", exception);
        }
    }

    /**
     * 禁止对远程主机关闭 TLS 校验，避免行情账户会话被中间人获取。
     *
     * @param baseUri Gateway 根地址
     * @param verifySsl 是否校验证书
     */
    private void validateTlsConfiguration(URI baseUri, boolean verifySsl) {
        if (verifySsl || !"https".equalsIgnoreCase(baseUri.getScheme())) {
            return;
        }
        String host = baseUri.getHost();
        if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host) && !"::1".equals(host)) {
            throw new IllegalStateException("IBKR TLS verification may only be disabled for a loopback Gateway URL");
        }
    }
}

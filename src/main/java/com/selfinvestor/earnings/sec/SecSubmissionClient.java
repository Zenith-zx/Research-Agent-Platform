package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import com.selfinvestor.earnings.sec.dto.SecSubmissionResponseDTO;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class SecSubmissionClient implements SecClient {

    private static final ZoneId SEC_ZONE = ZoneId.of("America/New_York");

    @Resource
    private WebClient.Builder webClientBuilder;

    @Resource
    private EarningsProperties properties;

    @Resource
    private SecRateLimiter rateLimiter;

    private WebClient client;

    /**
     * 初始化 SEC submissions API 客户端。
     */
    @PostConstruct
    void initialize() {
        EarningsProperties.Sec sec = properties.getSec();
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(sec.getConnectTimeout().toMillis()))
                .responseTimeout(sec.getReadTimeout());
        client = webClientBuilder.clone()
                .baseUrl(sec.getDataBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, sec.getUserAgent())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 查询指定公司的最近 Filing。
     *
     * @param company 关注公司
     * @return Filing 元数据列表
     */
    @Override
    public List<SecFilingMetadataDTO> recentFilings(CompanyPO company) {
        rateLimiter.acquire();
        SecSubmissionResponseDTO response = client.get()
                .uri("/submissions/CIK{cik}.json", company.getCik())
                .retrieve()
                .bodyToMono(SecSubmissionResponseDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> log.warn(
                                "SEC submissions request retry: ticker={} cik={} attempt={} reason={}",
                                company.getTicker(), company.getCik(), signal.totalRetries() + 1,
                                signal.failure().getMessage()))
                        .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
                .block();
        if (response == null || response.getFilings() == null || response.getFilings().getRecent() == null) {
            return Collections.emptyList();
        }
        return map(company, response.getFilings().getRecent());
    }

    /**
     * 将 SEC 并行字段列表转换为 Filing 元数据列表。
     *
     * @param company 关注公司
     * @param recent 最近 Filing 字段
     * @return Filing 元数据列表
     */
    private List<SecFilingMetadataDTO> map(CompanyPO company, SecSubmissionResponseDTO.RecentDTO recent) {
        List<SecFilingMetadataDTO> result = new ArrayList<>();
        int size = recent.getAccessionNumber() == null ? 0 : recent.getAccessionNumber().size();
        for (int i = 0; i < size; i++) {
            String accession = value(recent.getAccessionNumber(), i);
            String primary = value(recent.getPrimaryDocument(), i);
            String archiveUrl = properties.getSec().getBaseUrl() + "/Archives/edgar/data/" +
                    Long.parseLong(company.getCik()) + "/" + accession.replace("-", "") + "/" + primary;
            result.add(new SecFilingMetadataDTO(accession, value(recent.getForm(), i),
                    LocalDate.parse(value(recent.getFilingDate(), i)), parseAccepted(value(recent.getAcceptanceDateTime(), i)),
                    primary, value(recent.getItems(), i), archiveUrl));
        }
        return result;
    }

    /**
     * 安全读取列表指定位置的值。
     *
     * @param values 字符串列表
     * @param index 下标
     * @return 对应值或空字符串
     */
    private static String value(List<String> values, int index) {
        return values != null && index < values.size() ? values.get(index) : "";
    }

    /**
     * 将 SEC 接收时间转换为时间戳。
     *
     * @param value SEC 时间文本
     * @return 时间戳
     */
    private static Instant parseAccepted(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeException ignored) {
            return LocalDateTime.parse(value).atZone(SEC_ZONE).toInstant();
        }
    }

    /**
     * 判断 SEC 请求异常是否允许重试。
     *
     * @param error 请求异常
     * @return 是否允许重试
     */
    private boolean isRetryable(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
        }
        if (error instanceof WebClientRequestException) {
            return true;
        }
        return error instanceof TimeoutException;
    }
}

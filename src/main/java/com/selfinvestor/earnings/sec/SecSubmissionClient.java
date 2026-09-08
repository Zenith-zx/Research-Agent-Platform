package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import com.selfinvestor.earnings.sec.dto.SecSubmissionResponse;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecSubmissionClient implements SecClient {
    private static final ZoneId SEC_ZONE = ZoneId.of("America/New_York");
    private final WebClient client;
    private final EarningsProperties properties;
    private final SecRateLimiter rateLimiter;

    public SecSubmissionClient(WebClient.Builder builder, EarningsProperties properties, SecRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        EarningsProperties.Sec sec = properties.getSec();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(sec.getConnectTimeout().toMillis()))
                .responseTimeout(sec.getReadTimeout());
        this.client = builder.clone()
                .baseUrl(sec.getDataBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, sec.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public List<SecFilingMetadata> recentFilings(Company company) {
        rateLimiter.acquire();
        SecSubmissionResponse response = client.get()
                .uri("/submissions/CIK{cik}.json", company.getCik())
                .retrieve()
                .bodyToMono(SecSubmissionResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5)).filter(this::isRetryable))
                .block();
        if (response == null || response.filings() == null || response.filings().recent() == null) return List.of();
        return map(company, response.filings().recent());
    }

    private List<SecFilingMetadata> map(Company company, SecSubmissionResponse.Recent recent) {
        List<SecFilingMetadata> result = new ArrayList<>();
        int size = recent.accessionNumber() == null ? 0 : recent.accessionNumber().size();
        for (int i = 0; i < size; i++) {
            String accession = value(recent.accessionNumber(), i);
            String primary = value(recent.primaryDocument(), i);
            String archiveUrl = properties.getSec().getBaseUrl() + "/Archives/edgar/data/" +
                    Long.parseLong(company.getCik()) + "/" + accession.replace("-", "") + "/" + primary;
            result.add(new SecFilingMetadata(accession, value(recent.form(), i),
                    LocalDate.parse(value(recent.filingDate(), i)), parseAccepted(value(recent.acceptanceDateTime(), i)),
                    primary, value(recent.items(), i), archiveUrl));
        }
        return result;
    }

    private static String value(List<String> values, int index) {
        return values != null && index < values.size() ? values.get(index) : "";
    }

    private static Instant parseAccepted(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value); }
        catch (DateTimeException ignored) { return LocalDateTime.parse(value).atZone(SEC_ZONE).toInstant(); }
    }
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof org.springframework.web.reactive.function.client.WebClientResponseException response)) return true;
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

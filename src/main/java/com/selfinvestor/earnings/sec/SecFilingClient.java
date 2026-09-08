package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.Filing;
import com.selfinvestor.earnings.sec.dto.SecFilingDocument;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SecFilingClient {
    private static final Pattern DOCUMENT = Pattern.compile("(?is)<DOCUMENT>(.*?)</DOCUMENT>");
    private static final Pattern TEXT = Pattern.compile("(?is)<TEXT>(.*?)</TEXT>");
    private final WebClient client;
    private final EarningsProperties properties;
    private final SecRateLimiter rateLimiter;

    public SecFilingClient(WebClient.Builder builder, EarningsProperties properties, SecRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        EarningsProperties.Sec sec = properties.getSec();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(sec.getConnectTimeout().toMillis()))
                .responseTimeout(sec.getReadTimeout());
        this.client = builder.clone().baseUrl(sec.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, sec.getUserAgent())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder().codecs(c -> c.defaultCodecs().maxInMemorySize(25 * 1024 * 1024)).build())
                .build();
    }

    public List<SecFilingDocument> downloadDocuments(Filing filing) {
        rateLimiter.acquire();
        String cik = Long.toString(Long.parseLong(filing.getCompany().getCik()));
        String accessionCompact = filing.getAccessionNumber().replace("-", "");
        String submissionPath = "/Archives/edgar/data/" + cik + "/" + accessionCompact + "/" + filing.getAccessionNumber() + ".txt";
        String submission = client.get().uri(submissionPath).retrieve().bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5)).filter(this::isRetryable)).block();
        if (submission == null || submission.isBlank()) throw new IllegalStateException("SEC returned an empty filing");
        return parseSubmission(submission, cik, accessionCompact);
    }

    List<SecFilingDocument> parseSubmission(String submission, String cik, String accessionCompact) {
        List<SecFilingDocument> documents = new ArrayList<>();
        Matcher matcher = DOCUMENT.matcher(submission);
        while (matcher.find()) {
            String block = matcher.group(1);
            String filename = tag(block, "FILENAME");
            String content = text(block);
            if (filename.isBlank() || content.isBlank() || !isTextDocument(filename)) continue;
            String url = properties.getSec().getBaseUrl() + "/Archives/edgar/data/" + cik + "/" + accessionCompact + "/" + filename;
            documents.add(new SecFilingDocument(tag(block, "TYPE"), filename, tag(block, "DESCRIPTION"), url, content));
        }
        if (documents.isEmpty()) throw new IllegalArgumentException("No parseable text documents found in SEC submission");
        return documents;
    }

    private static String tag(String block, String tag) {
        Matcher matcher = Pattern.compile("(?im)^<" + tag + ">\\s*([^\\r\\n<]+)").matcher(block);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
    private static String text(String block) {
        Matcher matcher = TEXT.matcher(block);
        return matcher.find() ? matcher.group(1).trim() : block;
    }
    private static boolean isTextDocument(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".htm") || lower.endsWith(".html") || lower.endsWith(".xhtml") ||
                lower.endsWith(".xml") || lower.endsWith(".xsd") || lower.endsWith(".txt");
    }
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof org.springframework.web.reactive.function.client.WebClientResponseException response)) return true;
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

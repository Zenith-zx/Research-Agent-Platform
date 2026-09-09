package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.sec.dto.SecFilingDocumentDTO;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
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

    @Resource
    private WebClient.Builder webClientBuilder;

    @Resource
    private EarningsProperties properties;

    @Resource
    private SecRateLimiter rateLimiter;

    private WebClient client;

    /**
     * 初始化 SEC Filing 下载客户端。
     */
    @PostConstruct
    void initialize() {
        EarningsProperties.Sec sec = properties.getSec();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(sec.getConnectTimeout().toMillis()))
                .responseTimeout(sec.getReadTimeout());
        client = webClientBuilder.clone().baseUrl(sec.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, sec.getUserAgent())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder().codecs(c -> c.defaultCodecs().maxInMemorySize(25 * 1024 * 1024)).build())
                .build();
    }

    /**
     * 下载并拆解一份完整 SEC submission。
     *
     * @param filing Filing 实体
     * @return 可解析附件列表
     */
    public List<SecFilingDocumentDTO> downloadDocuments(FilingPO filing) {
        rateLimiter.acquire();
        String cik = Long.toString(Long.parseLong(filing.getCompany().getCik()));
        String accessionCompact = filing.getAccessionNumber().replace("-", "");
        String submissionPath = "/Archives/edgar/data/" + cik + "/" + accessionCompact + "/" + filing.getAccessionNumber() + ".txt";
        String submission = client.get().uri(submissionPath).retrieve().bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5)).filter(this::isRetryable)).block();
        if (submission == null || submission.isBlank()) {
            throw new IllegalStateException("SEC returned an empty filing");
        }
        return parseSubmission(submission, cik, accessionCompact);
    }

    /**
     * 从 SEC submission 文本中解析附件。
     *
     * @param submission submission 原文
     * @param cik 无前导零 CIK
     * @param accessionCompact 无连字符 accession number
     * @return 可解析附件列表
     */
    List<SecFilingDocumentDTO> parseSubmission(String submission, String cik, String accessionCompact) {
        List<SecFilingDocumentDTO> documents = new ArrayList<>();
        Matcher matcher = DOCUMENT.matcher(submission);
        while (matcher.find()) {
            String block = matcher.group(1);
            String filename = tag(block, "FILENAME");
            String content = text(block);
            if (filename.isBlank() || content.isBlank() || !isTextDocument(filename)) {
                continue;
            }
            String url = properties.getSec().getBaseUrl() + "/Archives/edgar/data/" + cik + "/" + accessionCompact + "/" + filename;
            documents.add(new SecFilingDocumentDTO(tag(block, "TYPE"), filename, tag(block, "DESCRIPTION"), url, content));
        }
        if (CollectionUtils.isEmpty(documents)) {
            throw new IllegalArgumentException("No parseable text documents found in SEC submission");
        }
        return documents;
    }

    /**
     * 提取 SEC 文档块中的标签值。
     *
     * @param block 文档块
     * @param tag 标签名称
     * @return 标签值
     */
    private static String tag(String block, String tag) {
        Matcher matcher = Pattern.compile("(?im)^<" + tag + ">\\s*([^\\r\\n<]+)").matcher(block);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
    /**
     * 提取 SEC 文档块中的正文。
     *
     * @param block 文档块
     * @return 文档正文
     */
    private static String text(String block) {
        Matcher matcher = TEXT.matcher(block);
        return matcher.find() ? matcher.group(1).trim() : block;
    }
    /**
     * 判断附件是否为可解析文本格式。
     *
     * @param filename 文件名
     * @return 是否可解析
     */
    private static boolean isTextDocument(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".htm") || lower.endsWith(".html") || lower.endsWith(".xhtml") ||
                lower.endsWith(".xml") || lower.endsWith(".xsd") || lower.endsWith(".txt");
    }
    /**
     * 判断 SEC 下载异常是否允许重试。
     *
     * @param error 请求异常
     * @return 是否允许重试
     */
    private boolean isRetryable(Throwable error) {
        if (!(error instanceof org.springframework.web.reactive.function.client.WebClientResponseException response)) {
            return true;
        }
        return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
    }
}

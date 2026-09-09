package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.event.FilingDownloadedEvent;
import com.selfinvestor.earnings.event.FilingParsedEvent;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.filing.FilingDocumentPO;
import com.selfinvestor.earnings.filing.FilingDocumentSelector;
import com.selfinvestor.earnings.filing.FilingParser;
import com.selfinvestor.earnings.repository.FilingDocumentRepository;
import com.selfinvestor.earnings.repository.FilingRepository;
import com.selfinvestor.earnings.sec.SecFilingClient;
import com.selfinvestor.earnings.sec.dto.SecFilingDocumentDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@Slf4j
public class FilingProcessingService {
    @Resource
    private FilingRepository filings;
    @Resource
    private FilingDocumentRepository documents;
    @Resource
    private SecFilingClient secClient;
    @Resource
    private FilingDocumentSelector selector;
    @Resource
    private FilingParser parser;
    @Resource
    private ApplicationEventPublisher events;
    @Resource
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactions;

    /**
     * 初始化事务模板。
     */
    @PostConstruct
    void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 下载 Filing 的可解析附件。
     *
     * @param filingId Filing 主键
     */
    public void download(Long filingId) {
        FilingPO filing = filings.findOneById(filingId).orElseThrow();
        transition(filingId, FilingStatus.DOWNLOADING);
        List<SecFilingDocumentDTO> downloaded = secClient.downloadDocuments(filing);
        transactions.executeWithoutResult(status -> {
            for (SecFilingDocumentDTO item : downloaded) {
                documents.insert(new FilingDocumentPO(filing, item.getType(), item.getFilename(), item.getDescription(),
                        item.getUrl(), item.getContent()));
            }
            filings.updateStatus(filingId, FilingStatus.DOWNLOADED, null);
            events.publishEvent(new FilingDownloadedEvent(filingId));
        });
        log.info("Filing downloaded: {} accession={} documents={}", filing.getCompany().getTicker(), filing.getAccessionNumber(), downloaded.size());
    }

    /**
     * 选择并解析 Filing 的高价值附件。
     *
     * @param filingId Filing 主键
     */
    public void parse(Long filingId) {
        transition(filingId, FilingStatus.PARSING);
        FilingPO filing = filings.findById(filingId).orElseThrow();
        List<FilingDocumentPO> all = documents.findAllByFilingIdOrderById(filingId);
        List<FilingDocumentPO> selected = selector.select(all, filing.getFormType());
        if (CollectionUtils.isEmpty(selected)) {
            throw new IllegalArgumentException("No relevant earnings document found");
        }
        transactions.executeWithoutResult(status -> {
            for (FilingDocumentPO document : selected) {
                String clean = parser.parse(document.getRawContent());
                document.parsed(clean, true);
                documents.updateParsed(document);
                log.info("Earnings document selected: type={} name={}", document.getDocumentType(), document.getDocumentName());
                log.info("Filing parsed, chars={}", clean.length());
            }
            filings.updateStatus(filingId, FilingStatus.PARSED, null);
            events.publishEvent(new FilingParsedEvent(filingId));
        });
    }

    /**
     * 将 Filing 标记为失败。
     *
     * @param filingId Filing 主键
     * @param exception 处理异常
     */
    public void fail(Long filingId, Exception exception) {
        String message = exception.getMessage() == null ? "Unknown error" : exception.getMessage();
        transactions.executeWithoutResult(status -> filings.updateStatus(filingId, FilingStatus.FAILED,
                message.substring(0, Math.min(2000, message.length()))));
        log.error("Filing processing failed: filingId={} error={}", filingId, exception.getMessage(), exception);
    }

    /**
     * 切换 Filing 处理状态。
     *
     * @param filingId Filing 主键
     * @param status 新状态
     */
    private void transition(Long filingId, FilingStatus status) {
        transactions.executeWithoutResult(tx -> filings.updateStatus(filingId, status, null));
    }
}

package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.event.FilingDownloadedEvent;
import com.selfinvestor.earnings.event.FilingParsedEvent;
import com.selfinvestor.earnings.sec.SecFilingClient;
import com.selfinvestor.earnings.sec.dto.SecFilingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class FilingProcessingService {
    private static final Logger log = LoggerFactory.getLogger(FilingProcessingService.class);
    private final FilingRepository filings;
    private final FilingDocumentRepository documents;
    private final SecFilingClient secClient;
    private final FilingDocumentSelector selector;
    private final FilingParser parser;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;

    public FilingProcessingService(FilingRepository filings, FilingDocumentRepository documents, SecFilingClient secClient,
                                   FilingDocumentSelector selector, FilingParser parser, ApplicationEventPublisher events,
                                   PlatformTransactionManager transactionManager) {
        this.filings = filings; this.documents = documents; this.secClient = secClient; this.selector = selector;
        this.parser = parser; this.events = events; this.transactions = new TransactionTemplate(transactionManager);
    }

    public void download(Long filingId) {
        Filing filing = filings.findOneById(filingId).orElseThrow();
        transition(filingId, FilingStatus.DOWNLOADING);
        List<SecFilingDocument> downloaded = secClient.downloadDocuments(filing);
        transactions.executeWithoutResult(status -> {
            Filing managed = filings.findById(filingId).orElseThrow();
            for (SecFilingDocument item : downloaded) {
                documents.save(new FilingDocument(managed, item.type(), item.filename(), item.description(), item.url(), item.content()));
            }
            managed.transitionTo(FilingStatus.DOWNLOADED);
            events.publishEvent(new FilingDownloadedEvent(filingId));
        });
        log.info("Filing downloaded: {} accession={} documents={}", filing.getCompany().getTicker(), filing.getAccessionNumber(), downloaded.size());
    }

    public void parse(Long filingId) {
        transition(filingId, FilingStatus.PARSING);
        Filing filing = filings.findById(filingId).orElseThrow();
        List<FilingDocument> all = documents.findAllByFilingIdOrderById(filingId);
        List<FilingDocument> selected = selector.select(all, filing.getFormType());
        if (selected.isEmpty()) throw new IllegalArgumentException("No relevant earnings document found");
        transactions.executeWithoutResult(status -> {
            for (FilingDocument document : selected) {
                String clean = parser.parse(document.getRawContent());
                document.parsed(clean, true);
                documents.save(document);
                log.info("Earnings document selected: type={} name={}", document.getDocumentType(), document.getDocumentName());
                log.info("Filing parsed, chars={}", clean.length());
            }
            Filing managed = filings.findById(filingId).orElseThrow();
            managed.transitionTo(FilingStatus.PARSED);
            events.publishEvent(new FilingParsedEvent(filingId));
        });
    }

    public void fail(Long filingId, Exception exception) {
        transactions.executeWithoutResult(status -> filings.findById(filingId).ifPresent(filing -> filing.fail(exception.getMessage())));
        log.error("Filing processing failed: filingId={} error={}", filingId, exception.getMessage(), exception);
    }

    private void transition(Long filingId, FilingStatus status) {
        transactions.executeWithoutResult(tx -> filings.findById(filingId).orElseThrow().transitionTo(status));
    }
}

package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import com.selfinvestor.earnings.filing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AiAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private final FilingRepository filings;
    private final FilingDocumentRepository documents;
    private final AiAnalysisRepository analyses;
    private final FilingContentProcessor processor;
    private final OpenAiClient client;
    private final ObjectMapper mapper;
    private final EarningsProperties properties;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;
    private final Executor analysisExecutor;

    public AiAnalysisService(FilingRepository filings, FilingDocumentRepository documents, AiAnalysisRepository analyses,
                             FilingContentProcessor processor, OpenAiClient client, ObjectMapper mapper,
                             EarningsProperties properties, ApplicationEventPublisher events,
                             PlatformTransactionManager transactionManager, @Qualifier("analysisExecutor") Executor analysisExecutor) {
        this.filings = filings; this.documents = documents; this.analyses = analyses; this.processor = processor;
        this.client = client; this.mapper = mapper; this.properties = properties; this.events = events;
        this.transactions = transactionManager == null ? null : new TransactionTemplate(transactionManager); this.analysisExecutor = analysisExecutor;
    }

    public void analyze(Long filingId) {
        Filing filing = filings.findOneById(filingId).orElseThrow();
        List<FilingDocument> selected = documents.findAllByFilingIdAndSelectedForAnalysisTrue(filingId);
        transactions.executeWithoutResult(tx -> filings.findById(filingId).orElseThrow().transitionTo(FilingStatus.ANALYZING));
        log.info("OpenAI analysis started: {} accession={}", filing.getCompany().getTicker(), filing.getAccessionNumber());
        Instant started = Instant.now();
        FinancialAnalysis result = analyzeContent(filing, selected);
        String json;
        try { json = mapper.writeValueAsString(result); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize analysis", exception); }
        transactions.executeWithoutResult(tx -> {
            Filing managed = filings.findById(filingId).orElseThrow();
            AiAnalysis saved = analyses.save(new AiAnalysis(managed, properties.getOpenai().getModel(), json, result.summary()));
            managed.transitionTo(FilingStatus.COMPLETED);
            events.publishEvent(new AiAnalysisCompletedEvent(filingId, saved.getId()));
        });
        log.info("OpenAI analysis completed, duration={}ms", Duration.between(started, Instant.now()).toMillis());
    }

    FinancialAnalysis analyzeContent(Filing filing, List<FilingDocument> selected) {
        List<FilingContentProcessor.ContentChunk> chunks = processor.process(selected);
        String context = "Company: " + filing.getCompany().getCompanyName() + "\nTicker: " + filing.getCompany().getTicker() +
                "\nForm: " + filing.getFormType() + "\nFiling date: " + filing.getFilingDate() + "\n\n";
        if (chunks.size() == 1) return client.analyze(context + chunks.get(0).content());
        List<CompletableFuture<FinancialAnalysis>> futures = chunks.stream().map(chunk ->
                CompletableFuture.supplyAsync(() -> client.analyze(context + "This is the " + chunk.type() + " chunk. Extract only facts present here.\n\n" + chunk.content()), analysisExecutor)).toList();
        List<FinancialAnalysis> partials = futures.stream().map(CompletableFuture::join).toList();
        try {
            return client.analyze(context + "Synthesize the following independently extracted chunk analyses. Resolve duplicates, retain nulls where unsupported, and do not invent consensus data.\n" + mapper.writeValueAsString(partials));
        } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize partial analyses", exception); }
    }
}

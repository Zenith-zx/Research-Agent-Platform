package com.selfinvestor.earnings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.AiAnalysisPO;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.common.exception.BusinessException;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import com.selfinvestor.earnings.filing.*;
import com.selfinvestor.earnings.repository.AiAnalysisRepository;
import com.selfinvestor.earnings.repository.FilingDocumentRepository;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiAnalysisService {

    @Resource
    private FilingRepository filings;

    @Resource
    private FilingDocumentRepository documents;

    @Resource
    private AiAnalysisRepository analyses;

    @Resource
    private FilingContentProcessor processor;

    @Resource
    private OpenAiClient client;

    @Resource
    private ObjectMapper mapper;

    @Resource
    private EarningsProperties properties;

    @Resource
    private ApplicationEventPublisher events;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource(name = "analysisExecutor")
    private Executor analysisExecutor;

    private TransactionTemplate transactions;

    /**
     * 初始化事务模板。
     */
    @PostConstruct
    void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 对指定 Filing 执行 AI 分析并持久化结果。
     *
     * @param filingId Filing 主键
     */
    public void analyze(Long filingId) {
        FilingPO filing = filings.findOneById(filingId).orElseThrow();
        List<FilingDocumentPO> selected = documents.findAllByFilingIdAndSelectedForAnalysisTrue(filingId);
        transactions.executeWithoutResult(tx -> filings.updateStatus(filingId, FilingStatus.ANALYZING, null));
        log.info("OpenAI analysis started: {} accession={}", filing.getCompany().getTicker(), filing.getAccessionNumber());
        Instant started = Instant.now();
        FinancialAnalysisDTO result = analyzeContent(filing, selected);
        String json;
        try {
            json = mapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize analysis", exception);
        }
        transactions.executeWithoutResult(tx -> {
            AiAnalysisPO saved = new AiAnalysisPO(filing, properties.getOpenai().getModel(), json, result.getSummary());
            analyses.insert(saved);
            filings.updateStatus(filingId, FilingStatus.COMPLETED, null);
            events.publishEvent(new AiAnalysisCompletedEvent(filingId, saved.getId()));
        });
        log.info("OpenAI analysis completed, duration={}ms", Duration.between(started, Instant.now()).toMillis());
    }

    /**
     * 根据已选择的文档执行单次或分块分析。
     *
     * @param filing Filing 实体
     * @param selected 已选择的文档
     * @return 结构化财报分析
     */
    protected FinancialAnalysisDTO analyzeContent(FilingPO filing, List<FilingDocumentPO> selected) {
        List<FilingContentProcessor.ContentChunk> chunks = processor.process(selected);
        String context = "Company: " + filing.getCompany().getCompanyName() + "\nTicker: " + filing.getCompany().getTicker() +
                "\nForm: " + filing.getFormType() + "\nFiling date: " + filing.getFilingDate() + "\n\n";
        if (chunks.size() == 1) {
            return client.analyze(context + chunks.get(0).getContent());
        }
        List<CompletableFuture<FinancialAnalysisDTO>> futures = chunks.stream().map(chunk ->
                CompletableFuture.supplyAsync(() -> client.analyze(context + "This is the " + chunk.getType()
                        + " chunk. Extract only facts present here.\n\n" + chunk.getContent()), analysisExecutor))
                .collect(Collectors.toList());
        List<FinancialAnalysisDTO> partials = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        try {
            return client.analyze(context + "Synthesize the following independently extracted chunk analyses. Resolve duplicates, retain nulls where unsupported, and do not invent consensus data.\n" + mapper.writeValueAsString(partials));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize partial analyses", exception);
        }
    }

    /**
     * 查询指定 Filing 的结构化分析结果。
     *
     * @param filingId Filing 主键
     * @return 结构化分析 JSON
     */
    public JsonNode findAnalysis(Long filingId) {
        AiAnalysisPO analysis = analyses.findByFilingId(filingId)
                .orElseThrow(() -> new BusinessException("Analysis not found for filing: " + filingId));
        try {
            return mapper.readTree(analysis.getAnalysisJson());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored analysis is invalid: " + filingId, exception);
        }
    }
}

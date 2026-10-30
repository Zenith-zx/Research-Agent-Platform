package com.selfinvestor.earnings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.earnings.EarningsAgentService;
import com.selfinvestor.earnings.agent.earnings.EarningsConclusionAdapter;
import com.selfinvestor.earnings.agent.earnings.EarningsEvent;
import com.selfinvestor.earnings.agent.earnings.model.EarningsConclusion;
import com.selfinvestor.earnings.agent.runtime.submission.AgentSubmissionService;
import com.selfinvestor.earnings.analysis.AiAnalysisPO;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import com.selfinvestor.earnings.filing.FilingContentProcessor;
import com.selfinvestor.earnings.filing.FilingDocumentPO;
import com.selfinvestor.earnings.filing.FilingPO;
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

import java.util.List;

/**
 * 财报分析模式 facade：保留旧 Chain，并在 Graph 模式下使用 LongGraph Agent。
 */
@Service
@Slf4j
public class EarningsAnalysisService {
    @Resource
    private AiAnalysisService legacyAnalysis;

    @Resource
    private EarningsAgentService agent;

    @Resource
    private EarningsConclusionAdapter adapter;

    @Resource
    private FilingRepository filings;

    @Resource
    private FilingDocumentRepository documents;

    @Resource
    private FilingContentProcessor processor;

    @Resource
    private AiAnalysisRepository analyses;

    @Resource
    private EarningsProperties properties;

    @Resource
    private AgentRuntimeProperties runtimeProperties;

    @Resource
    private AgentSubmissionService submissions;

    @Resource
    private ObjectMapper mapper;

    @Resource
    private ApplicationEventPublisher events;

    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactions;

    /**
     * 初始化用于分隔状态更新和最终结果保存的事务模板。
     */
    @PostConstruct
    void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 按配置选择 Graph 或旧 Chain 分析指定 Filing。
     *
     * @param filingId Filing 主键
     */
    public void analyze(Long filingId) {
        if (runtimeProperties.isEnabled()) {
            log.info("Dynamic Agent Runtime selected: filingId={}", filingId);
            submissions.submitEarnings(filingId);
            return;
        }
        if (properties.getAnalysisMode() == EarningsProperties.AnalysisMode.CHAIN) {
            log.info("Legacy earnings analysis selected: filingId={}", filingId);
            legacyAnalysis.analyze(filingId);
            return;
        }
        analyzeWithGraph(filingId);
    }

    /**
     * 加载已解析财报、执行 Earnings Graph 并保存兼容结果。
     *
     * @param filingId Filing 主键
     */
    private void analyzeWithGraph(Long filingId) {
        FilingPO filing = filings.findOneById(filingId).orElseThrow();
        List<FilingDocumentPO> selected = documents.findAllByFilingIdAndSelectedForAnalysisTrue(filingId);
        transactions.executeWithoutResult(tx -> filings.updateStatus(filingId, FilingStatus.ANALYZING, null));
        EarningsEvent event = new EarningsEvent(
                filing.getCompany().getTicker(),
                filing.getCompany().getCompanyName(),
                filing.getId(),
                filing.getAccessionNumber(),
                filing.getFormType(),
                filing.getFilingDate(),
                filing.getSecUrl(),
                buildSourceContent(selected));
        EarningsConclusion conclusion = agent.analyze(event);
        FinancialAnalysisDTO compatible = adapter.toLegacyDto(event, conclusion);
        String json = serialize(compatible);
        transactions.executeWithoutResult(tx -> {
            AiAnalysisPO saved = new AiAnalysisPO(filing, properties.getOpenai().getModel(), json,
                    compatible.getSummary());
            analyses.insert(saved);
            filings.updateStatus(filingId, FilingStatus.COMPLETED, null);
            events.publishEvent(new AiAnalysisCompletedEvent(filingId, saved.getId()));
        });
    }

    /**
     * 将已有语义分块均衡压缩到单次 Agent 输入字符预算内。
     *
     * @param selected 已选择用于分析的 Filing 文档
     * @return 带分段标识的财报上下文
     */
    private String buildSourceContent(List<FilingDocumentPO> selected) {
        List<FilingContentProcessor.ContentChunk> chunks = processor.process(selected);
        int maxChars = properties.getOpenai().getMaxInputChars();
        int perChunk = Math.max(1, maxChars / Math.max(1, chunks.size()));
        StringBuilder content = new StringBuilder();
        for (FilingContentProcessor.ContentChunk chunk : chunks) {
            String header = "SECTION: " + chunk.getType() + "\n";
            int available = Math.max(0, Math.min(perChunk - header.length(), maxChars - content.length() - header.length()));
            if (available <= 0) {
                break;
            }
            String value = chunk.getContent();
            content.append(header).append(value, 0, Math.min(value.length(), available)).append("\n\n");
        }
        return content.toString();
    }

    /**
     * 将兼容 DTO 序列化为数据库 JSON。
     *
     * @param result 财报分析 DTO
     * @return JSON 字符串
     */
    private String serialize(FinancialAnalysisDTO result) {
        try {
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize graph analysis", exception);
        }
    }
}

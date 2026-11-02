package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.llm.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.submission.SubmissionType;
import com.selfinvestor.earnings.analysis.AiAnalysisPO;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.enums.FundamentalTrend;
import com.selfinvestor.earnings.event.AgentTurnCompletedEvent;
import com.selfinvestor.earnings.event.AgentTurnFailedEvent;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.AiAnalysisRepository;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;

/**
 * 将动态财报 Turn 的结果接回已有 AI_ANALYSIS 和通知链。
 */
@Component
@Slf4j
public class EarningsRuntimeCompletionListener {
    @Resource
    private FilingRepository filings;
    @Resource
    private AiAnalysisRepository analyses;
    @Resource
    private LlmClient llmClient;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private ApplicationEventPublisher events;
    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactions;

    /**
     * 初始化结果保存事务模板。
     */
    @PostConstruct
    void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 保存财报 Turn 的最终回答并复用原有通知事件。
     *
     * @param event Turn 完成事件
     */
    @EventListener
    public void onCompleted(AgentTurnCompletedEvent event) {
        if (!isEarnings(event.getSubmission())) {
            return;
        }
        Long filingId = filingId(event.getSubmission());
        FilingPO filing = filings.findOneById(filingId).orElseThrow();
        FinancialAnalysisDTO result = compatibleResult(filing, event.getTurn().getFinalResponse());
        String json = serialize(result);
        transactions.executeWithoutResult(status -> {
            AiAnalysisPO saved = new AiAnalysisPO(filing, llmClient.modelName(), json, result.getSummary());
            analyses.insert(saved);
            filings.updateStatus(filingId, FilingStatus.COMPLETED, null);
            events.publishEvent(new AiAnalysisCompletedEvent(filingId, saved.getId()));
        });
        log.info("Dynamic earnings agent result saved: filingId={} turn={}", filingId, event.getTurn().getId());
    }

    /**
     * 将财报 Turn 失败同步到 Filing 状态。
     *
     * @param event Turn 失败事件
     */
    @EventListener
    public void onFailed(AgentTurnFailedEvent event) {
        if (!isEarnings(event.getSubmission())) {
            return;
        }
        Long filingId = filingId(event.getSubmission());
        String message = event.getTurn().getFailureMessage();
        transactions.executeWithoutResult(status -> filings.updateStatus(filingId, FilingStatus.FAILED,
                truncate(message)));
        log.error("Dynamic earnings agent failed: filingId={} turn={} error={}", filingId,
                event.getTurn().getId(), message);
    }

    /**
     * 判断 Submission 是否来自财报发布事件。
     *
     * @param submission 外部输入
     * @return 是否为财报任务
     */
    private boolean isEarnings(Submission submission) {
        return submission != null && submission.getType() == SubmissionType.EARNINGS_RELEASED;
    }

    /**
     * 从 Submission metadata 读取 Filing 主键。
     *
     * @param submission 财报 Submission
     * @return Filing 主键
     */
    private Long filingId(Submission submission) {
        Object value = submission.getMetadata().get("filingId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.valueOf(value.toString());
        }
        throw new IllegalArgumentException("Earnings submission has no filingId");
    }

    /**
     * 将文本回答适配为现有查询和通知使用的 DTO。
     *
     * @param filing Filing 实体
     * @param response Agent 最终回答
     * @return 兼容 DTO
     */
    private FinancialAnalysisDTO compatibleResult(FilingPO filing, String response) {
        String headline = response.substring(0, Math.min(180, response.length()));
        return new FinancialAnalysisDTO(
                filing.getCompany().getCompanyName(),
                filing.getCompany().getTicker(),
                null,
                headline,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                FundamentalTrend.STABLE,
                "See the dynamic Agent Runtime analysis in summary.",
                false,
                "No automatic investment decision was made.",
                false,
                "Consensus data is only included when returned by an available Tool.",
                null,
                response);
    }

    /**
     * 将兼容 DTO 序列化为数据库 JSON。
     *
     * @param result 分析结果
     * @return JSON 文本
     */
    private String serialize(FinancialAnalysisDTO result) {
        try {
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize dynamic agent result", exception);
        }
    }

    /**
     * 限制 Filing 错误字段长度。
     *
     * @param message 原始错误
     * @return 安全错误文本
     */
    private String truncate(String message) {
        String value = message == null ? "Dynamic Agent Runtime failed" : message;
        return value.substring(0, Math.min(2000, value.length()));
    }
}

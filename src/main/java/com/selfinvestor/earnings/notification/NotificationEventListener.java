package com.selfinvestor.earnings.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.AiAnalysisPO;
import com.selfinvestor.earnings.repository.AiAnalysisRepository;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import com.selfinvestor.earnings.event.MarketStressAnalysisCompletedEvent;
import com.selfinvestor.earnings.marketstress.MarketStressAnalysisPO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.repository.MarketStressAnalysisRepository;
import com.selfinvestor.earnings.service.interfaces.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class NotificationEventListener {
    @Resource
    private AiAnalysisRepository analyses;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private MarketStressAnalysisRepository marketStressAnalyses;

    /**
     * 在 AI 分析事务提交后调用所有通知渠道。
     *
     * @param event AI 分析完成事件
     */
    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisCompleted(AiAnalysisCompletedEvent event) {
        AiAnalysisPO entity = analyses.findOneById(event.getAnalysisId()).orElseThrow();
        try {
            FinancialAnalysisDTO analysis = mapper.readValue(entity.getAnalysisJson(), FinancialAnalysisDTO.class);
            for (NotificationService service : applicationContext.getBeansOfType(NotificationService.class).values()) {
                try {
                    service.send(entity.getFiling(), analysis);
                } catch (Exception exception) {
                    log.error("Notification failed: service={} filingId={} error={}",
                            service.getClass().getSimpleName(), event.getFilingId(), exception.getMessage());
                }
            }
        } catch (Exception exception) {
            log.error("Cannot deserialize saved analysis for notification: analysisId={}", event.getAnalysisId(), exception);
        }
    }

    /**
     * 在市场压力分析事务提交后调用支持该消息的通知渠道。
     *
     * @param event 市场压力分析完成事件
     */
    @Async("marketStressExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketStressCompleted(MarketStressAnalysisCompletedEvent event) {
        MarketStressAnalysisPO entity = marketStressAnalyses.findById(event.getAnalysisId()).orElseThrow();
        try {
            MarketStressAnalysisDTO analysis = mapper.readValue(entity.getAnalysisJson(), MarketStressAnalysisDTO.class);
            for (NotificationService service : applicationContext.getBeansOfType(NotificationService.class).values()) {
                try {
                    service.sendMarketStress(entity, analysis);
                } catch (Exception exception) {
                    log.error("Market stress notification failed: service={} analysisId={} error={}",
                            service.getClass().getSimpleName(), event.getAnalysisId(), exception.getMessage());
                }
            }
        } catch (Exception exception) {
            log.error("Cannot deserialize market stress analysis for notification: analysisId={}",
                    event.getAnalysisId(), exception);
        }
    }
}

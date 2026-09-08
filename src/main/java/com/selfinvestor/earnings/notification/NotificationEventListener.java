package com.selfinvestor.earnings.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.AiAnalysisRepository;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.event.AiAnalysisCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final AiAnalysisRepository analyses;
    private final ObjectMapper mapper;
    private final List<NotificationService> services;

    public NotificationEventListener(AiAnalysisRepository analyses, ObjectMapper mapper, List<NotificationService> services) {
        this.analyses = analyses; this.mapper = mapper; this.services = services;
    }

    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisCompleted(AiAnalysisCompletedEvent event) {
        var entity = analyses.findOneById(event.analysisId()).orElseThrow();
        try {
            FinancialAnalysis analysis = mapper.readValue(entity.getAnalysisJson(), FinancialAnalysis.class);
            for (NotificationService service : services) {
                try { service.send(entity.getFiling(), analysis); }
                catch (Exception exception) { log.error("Notification failed: service={} filingId={} error={}", service.getClass().getSimpleName(), event.filingId(), exception.getMessage()); }
            }
        } catch (Exception exception) {
            log.error("Cannot deserialize saved analysis for notification: analysisId={}", event.analysisId(), exception);
        }
    }
}

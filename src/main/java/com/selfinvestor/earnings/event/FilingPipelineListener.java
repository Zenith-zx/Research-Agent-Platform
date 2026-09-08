package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.filing.FilingProcessingService;
import com.selfinvestor.earnings.analysis.AiAnalysisService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FilingPipelineListener {
    private final FilingProcessingService processing;
    private final AiAnalysisService analysis;
    public FilingPipelineListener(FilingProcessingService processing, AiAnalysisService analysis) {
        this.processing = processing; this.analysis = analysis;
    }

    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDetected(FilingDetectedEvent event) {
        try { processing.download(event.filingId()); }
        catch (Exception exception) { processing.fail(event.filingId(), exception); }
    }

    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDownloaded(FilingDownloadedEvent event) {
        try { processing.parse(event.filingId()); }
        catch (Exception exception) { processing.fail(event.filingId(), exception); }
    }

    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParsed(FilingParsedEvent event) {
        try { analysis.analyze(event.filingId()); }
        catch (Exception exception) { processing.fail(event.filingId(), exception); }
    }
}

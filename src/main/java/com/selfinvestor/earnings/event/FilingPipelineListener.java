package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.service.FilingProcessingService;
import com.selfinvestor.earnings.service.EarningsAnalysisService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FilingPipelineListener {

    @Resource
    private FilingProcessingService filingProcessingService;

    @Resource
    private EarningsAnalysisService analysis;

    /**
     * 下载新发现的 Filing。
     *
     * @param event Filing 发现事件
     */
    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDetected(FilingDetectedEvent event) {
        try {
            filingProcessingService.download(event.getFilingId());
        } catch (Exception exception) {
            filingProcessingService.fail(event.getFilingId(), exception);
        }
    }

    /**
     * 解析下载完成的 Filing。
     *
     * @param event Filing 下载完成事件
     */
    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDownloaded(FilingDownloadedEvent event) {
        try {
            filingProcessingService.parse(event.getFilingId());
        } catch (Exception exception) {
            filingProcessingService.fail(event.getFilingId(), exception);
        }
    }

    /**
     * 分析解析完成的 Filing。
     *
     * @param event Filing 解析完成事件
     */
    @Async("filingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParsed(FilingParsedEvent event) {
        try {
            analysis.analyze(event.getFilingId());
        } catch (Exception exception) {
            filingProcessingService.fail(event.getFilingId(), exception);
        }
    }
}

package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.service.MarketStressService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 市场压力事件处理链。
 */
@Component
public class MarketStressPipelineListener {
    @Resource
    private MarketStressService service;

    /**
     * 在超卖检测事务提交后准备市场上下文。
     *
     * @param event 超卖检测事件
     */
    @Async("marketStressExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOversold(OversoldDetectedEvent event) {
        try {
            service.prepareContext(event);
        } catch (Exception exception) {
            service.fail(event.getAnalysisId(), exception);
        }
    }

    /**
     * 在上下文事务提交后执行 AI 分析。
     *
     * @param event 上下文准备事件
     */
    @Async("marketStressExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContextReady(MarketContextReadyEvent event) {
        try {
            service.analyze(event.getAnalysisId());
        } catch (Exception exception) {
            service.fail(event.getAnalysisId(), exception);
        }
    }
}

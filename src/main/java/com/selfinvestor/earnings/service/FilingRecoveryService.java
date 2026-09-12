package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 从数据库恢复事务已提交但异步事件未执行的近期 Filing。
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "earnings.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FilingRecoveryService {

    @Resource
    private FilingRepository filingRepository;

    @Resource
    private FilingProcessingService filingProcessingService;

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private EarningsProperties properties;

    /**
     * 应用启动完成后分批恢复近期未完成 Filing。
     */
    @Async("filingExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void recoverIncompleteFilings() {
        int lookbackDays = Math.max(0, properties.getMonitor().getFilingLookbackDays());
        int batchSize = Math.max(1, properties.getMonitor().getRecoveryBatchSize());
        LocalDate cutoffDate = LocalDate.now(ZoneOffset.UTC).minusDays(lookbackDays);
        List<FilingPO> filings = filingRepository.findRecoverable(cutoffDate, batchSize);
        if (CollectionUtils.isEmpty(filings)) {
            log.info("No incomplete recent Filing requires recovery");
            return;
        }
        log.warn("Recovering incomplete Filing tasks: count={} cutoffDate={}", filings.size(), cutoffDate);
        for (FilingPO filing : filings) {
            try {
                resume(filing);
            } catch (Exception exception) {
                filingProcessingService.fail(filing.getId(), exception);
            }
        }
    }

    /**
     * 根据持久化状态从正确的处理步骤恢复 Filing。
     *
     * @param filing 待恢复 Filing
     */
    private void resume(FilingPO filing) {
        FilingStatus status = filing.getStatus();
        switch (status) {
            case DETECTED:
            case DOWNLOADING:
                filingProcessingService.download(filing.getId());
                break;
            case DOWNLOADED:
            case PARSING:
                filingProcessingService.parse(filing.getId());
                break;
            case PARSED:
            case ANALYZING:
                aiAnalysisService.analyze(filing.getId());
                break;
            default:
                log.debug("Filing status does not require recovery: filingId={} status={}", filing.getId(), status);
                break;
        }
    }
}

package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.event.FilingDetectedEvent;
import com.selfinvestor.earnings.event.FilingDownloadedEvent;
import com.selfinvestor.earnings.event.FilingParsedEvent;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Component
@Slf4j
public class FilingRecoveryService {
    @Resource
    private FilingRepository filings;
    @Resource
    private ApplicationEventPublisher events;

    /**
     * 应用启动后恢复被中断的 Filing 处理链路。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedFilings() {
        List<FilingPO> interrupted = filings.findAllByStatusIn(EnumSet.of(FilingStatus.DETECTED, FilingStatus.DOWNLOADING,
                FilingStatus.DOWNLOADED, FilingStatus.PARSING, FilingStatus.PARSED, FilingStatus.ANALYZING));
        for (FilingPO filing : interrupted) {
            switch (filing.getStatus()) {
                case DETECTED:
                case DOWNLOADING:
                    events.publishEvent(new FilingDetectedEvent(filing.getId()));
                    break;
                case DOWNLOADED:
                case PARSING:
                    events.publishEvent(new FilingDownloadedEvent(filing.getId()));
                    break;
                case PARSED:
                case ANALYZING:
                    events.publishEvent(new FilingParsedEvent(filing.getId()));
                    break;
                default:
                    break;
            }
        }
        if (CollectionUtils.isNotEmpty(interrupted)) {
            log.info("Recovered {} interrupted filing pipelines", interrupted.size());
        }
    }
}

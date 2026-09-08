package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.event.FilingDetectedEvent;
import com.selfinvestor.earnings.event.FilingDownloadedEvent;
import com.selfinvestor.earnings.event.FilingParsedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Component
public class FilingRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(FilingRecoveryService.class);
    private final FilingRepository filings;
    private final ApplicationEventPublisher events;

    public FilingRecoveryService(FilingRepository filings, ApplicationEventPublisher events) {
        this.filings = filings; this.events = events;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedFilings() {
        var interrupted = filings.findAllByStatusIn(EnumSet.of(FilingStatus.DETECTED, FilingStatus.DOWNLOADING,
                FilingStatus.DOWNLOADED, FilingStatus.PARSING, FilingStatus.PARSED, FilingStatus.ANALYZING));
        for (Filing filing : interrupted) {
            switch (filing.getStatus()) {
                case DETECTED, DOWNLOADING -> events.publishEvent(new FilingDetectedEvent(filing.getId()));
                case DOWNLOADED, PARSING -> events.publishEvent(new FilingDownloadedEvent(filing.getId()));
                case PARSED, ANALYZING -> events.publishEvent(new FilingParsedEvent(filing.getId()));
                default -> { }
            }
        }
        if (!interrupted.isEmpty()) log.info("Recovered {} interrupted filing pipelines", interrupted.size());
    }
}

package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.event.FilingDetectedEvent;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
public class FilingService {
    private static final Logger log = LoggerFactory.getLogger(FilingService.class);
    private final FilingRepository repository;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;

    public FilingService(FilingRepository repository, ApplicationEventPublisher events, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public Optional<Filing> registerIfNew(Company company, SecFilingMetadata metadata) {
        if (repository.existsByAccessionNumber(metadata.accessionNumber())) return Optional.empty();
        try {
            Filing filing = transactions.execute(status -> {
                Filing saved = repository.saveAndFlush(new Filing(company, metadata.accessionNumber(), metadata.formType(),
                        metadata.filingDate(), metadata.acceptedAt(), metadata.primaryDocument(), metadata.filingUrl()));
                events.publishEvent(new FilingDetectedEvent(saved.getId()));
                return saved;
            });
            log.info("Filing detected: {} {} accession={}", company.getTicker(), metadata.formType(), metadata.accessionNumber());
            return Optional.ofNullable(filing);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Duplicate filing ignored: accession={}", metadata.accessionNumber());
            return Optional.empty();
        }
    }
}

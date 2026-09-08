package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.company.CompanyRepository;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FilingDeduplicationTest {
    @Autowired FilingRepository filings;
    @Autowired CompanyRepository companies;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void sameAccessionNumberIsStoredOnlyOnce() {
        Company company = companies.saveAndFlush(new Company("CRDO", "0001807794", "Credo Technology", true));
        SecFilingMetadata metadata = new SecFilingMetadata("0001807794-26-000001", "8-K", LocalDate.now(), Instant.now(), "release.htm", "2.02", "url");
        FilingService service = new FilingService(filings, events, transactionManager);
        assertThat(service.registerIfNew(company, metadata)).isPresent();
        assertThat(service.registerIfNew(company, metadata)).isEmpty();
        assertThat(filings.count()).isOne();
    }
}

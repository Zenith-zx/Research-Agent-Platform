package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.repository.CompanyRepository;
import com.selfinvestor.earnings.repository.FilingRepository;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import com.selfinvestor.earnings.service.FilingService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FilingDeduplicationTest {
    @Resource
    private FilingRepository filings;
    @Resource
    private CompanyRepository companies;
    @Resource
    private FilingService service;

    /**
     * 验证相同 accession number 只会保存一次。
     */
    @Test
    void sameAccessionNumberIsStoredOnlyOnce() {
        CompanyPO company = new CompanyPO("CRDO", "0001807794", "Credo Technology", true);
        companies.insert(company);
        SecFilingMetadataDTO metadata = new SecFilingMetadataDTO("0001807794-26-000001", "8-K", LocalDate.now(),
                Instant.now(), "release.htm", "2.02", "url");
        FilingPO created = service.registerIfNew(company, metadata).orElseThrow();
        assertThat(created.getId()).isNotNull();
        assertThat(filings.findOneById(created.getId()).orElseThrow().getCompany().getTicker()).isEqualTo("CRDO");
        assertThat(service.registerIfNew(company, metadata)).isNotPresent();
        assertThat(filings.count()).isOne();
    }
}

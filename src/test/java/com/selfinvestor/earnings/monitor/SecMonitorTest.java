package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.filing.FilingService;
import com.selfinvestor.earnings.sec.SecClient;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

class SecMonitorTest {
    @Test
    void onlyRegistersPeriodicAndEarningsRelated8kFilings() {
        Company company = new Company("CRDO", "0001807794", "Credo", true);
        SecClient sec = mock(SecClient.class);
        FilingService filingService = mock(FilingService.class);
        SecFilingMetadata earnings8k = filing("a", "8-K", "2.02,9.01");
        SecFilingMetadata unrelated8k = filing("b", "8-K", "5.02");
        SecFilingMetadata tenQ = filing("c", "10-Q", "");
        when(sec.recentFilings(company)).thenReturn(List.of(earnings8k, unrelated8k, tenQ));

        new SecFilingMonitor(sec, filingService).poll(company);

        verify(filingService).registerIfNew(company, earnings8k);
        verify(filingService).registerIfNew(company, tenQ);
        verify(filingService, never()).registerIfNew(company, unrelated8k);
    }

    private static SecFilingMetadata filing(String accession, String form, String items) {
        return new SecFilingMetadata(accession, form, LocalDate.now(), Instant.now(), "main.htm", items, "https://sec/test");
    }
}

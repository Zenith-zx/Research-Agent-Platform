package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.service.FilingService;
import com.selfinvestor.earnings.sec.SecClient;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.Mockito.*;

class SecMonitorTest {
    /**
     * 验证监听器只注册定期报告和财报相关 8-K。
     */
    @Test
    void onlyRegistersPeriodicAndEarningsRelated8kFilings() {
        CompanyPO company = new CompanyPO("CRDO", "0001807794", "Credo", true);
        SecClient sec = mock(SecClient.class);
        FilingService filingService = mock(FilingService.class);
        SecFilingMetadataDTO earnings8k = filing("a", "8-K", "2.02,9.01");
        SecFilingMetadataDTO unrelated8k = filing("b", "8-K", "5.02");
        SecFilingMetadataDTO tenQ = filing("c", "10-Q", "");
        SecFilingMetadataDTO oldTenK = new SecFilingMetadataDTO("d", "10-K", LocalDate.now().minusDays(31),
                Instant.now(), "main.htm", "", "https://sec/test");
        when(sec.recentFilings(company)).thenReturn(Arrays.asList(earnings8k, unrelated8k, tenQ, oldTenK));

        SecFilingMonitor monitor = new SecFilingMonitor();
        EarningsProperties properties = new EarningsProperties();
        properties.getMonitor().setFilingLookbackDays(30);
        ReflectionTestUtils.setField(monitor, "secClient", sec);
        ReflectionTestUtils.setField(monitor, "filingService", filingService);
        ReflectionTestUtils.setField(monitor, "properties", properties);
        monitor.poll(company);

        verify(filingService).registerIfNew(company, earnings8k);
        verify(filingService).registerIfNew(company, tenQ);
        verify(filingService, never()).registerIfNew(company, unrelated8k);
        verify(filingService, never()).registerIfNew(company, oldTenK);
    }

    /**
     * 创建测试 Filing 元数据。
     *
     * @param accession accession number
     * @param form 表单类型
     * @param items 8-K 条目
     * @return Filing 元数据
     */
    private static SecFilingMetadataDTO filing(String accession, String form, String items) {
        return new SecFilingMetadataDTO(accession, form, LocalDate.now(), Instant.now(), "main.htm", items, "https://sec/test");
    }
}

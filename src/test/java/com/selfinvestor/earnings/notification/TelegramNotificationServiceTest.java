package com.selfinvestor.earnings.notification;

import com.selfinvestor.earnings.enums.FundamentalTrend;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.service.TelegramNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;

class TelegramNotificationServiceTest {
    /**
     * 验证关闭 Telegram 时不会发起网络请求。
     */
    @Test
    void disabledTelegramDoesNotCallNetwork() {
        EarningsProperties properties = new EarningsProperties();
        properties.getTelegram().setEnabled(false);
        TelegramNotificationService service = new TelegramNotificationService();
        ReflectionTestUtils.setField(service, "webClientBuilder", WebClient.builder());
        ReflectionTestUtils.setField(service, "properties", properties);
        service.initialize();
        FilingPO filing = new FilingPO(new CompanyPO("MU", "0000723125", "Micron", true), "a", "10-Q", LocalDate.now(), Instant.now(), "a.htm", "url");
        FinancialAnalysisDTO analysis = new FinancialAnalysisDTO("Micron", "MU", null, "headline", null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), FundamentalTrend.STABLE, "reason", false, null, false,
                "no consensus", 7, "summary");
        assertThatCode(() -> service.send(filing, analysis)).doesNotThrowAnyException();
    }
}

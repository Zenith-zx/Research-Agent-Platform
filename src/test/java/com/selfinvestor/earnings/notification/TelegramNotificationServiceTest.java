package com.selfinvestor.earnings.notification;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.analysis.dto.FundamentalTrend;
import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.Filing;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class TelegramNotificationServiceTest {
    @Test
    void disabledTelegramDoesNotCallNetwork() {
        EarningsProperties properties = new EarningsProperties();
        properties.getTelegram().setEnabled(false);
        TelegramNotificationService service = new TelegramNotificationService(WebClient.builder(), properties);
        Filing filing = new Filing(new Company("MU", "0000723125", "Micron", true), "a", "10-Q", LocalDate.now(), Instant.now(), "a.htm", "url");
        FinancialAnalysis analysis = new FinancialAnalysis("Micron", "MU", null, "headline", null, null, List.of(), List.of(), List.of(), List.of(), List.of(), FundamentalTrend.STABLE, "reason", false, null, false, "no consensus", 7, "summary");
        assertThatCode(() -> service.send(filing, analysis)).doesNotThrowAnyException();
    }
}

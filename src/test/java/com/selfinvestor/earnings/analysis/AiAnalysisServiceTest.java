package com.selfinvestor.earnings.analysis;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.analysis.dto.FundamentalTrend;
import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.filing.Filing;
import com.selfinvestor.earnings.filing.FilingContentProcessor;
import com.selfinvestor.earnings.filing.FilingDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class AiAnalysisServiceTest {
    @Test
    void sendsParsedDocumentToOpenAiAndReturnsStructuredResult() {
        EarningsProperties properties = new EarningsProperties();
        properties.getOpenai().setMaxInputChars(60000);
        OpenAiClient openAi = mock(OpenAiClient.class);
        FinancialAnalysis expected = new FinancialAnalysis("NVIDIA", "NVDA", "Q2", "Growth", null, null,
                List.of(), List.of("Revenue growth"), List.of(), List.of(), List.of(), FundamentalTrend.IMPROVING,
                "Growth accelerated", false, null, false, "当前版本没有接入分析师一致预期，因此无法判断 Beat/Miss。", 8, "Strong quarter");
        when(openAi.analyze(contains("Revenue was $30 billion"))).thenReturn(expected);
        Company company = new Company("NVDA", "0001045810", "NVIDIA", true);
        Filing filing = new Filing(company, "0001-24-000001", "10-Q", LocalDate.now(), Instant.now(), "q.htm", "url");
        FilingDocument document = new FilingDocument(filing, "10-Q", "q.htm", "Quarterly report", "url", "raw");
        document.parsed("Revenue was $30 billion", true);

        FinancialAnalysis result = new TestableService(new FilingContentProcessor(properties), openAi).call(filing, List.of(document));
        assertThat(result).isSameAs(expected);
        verify(openAi).analyze(contains("Ticker: NVDA"));
    }

    private static class TestableService extends AiAnalysisService {
        TestableService(FilingContentProcessor processor, OpenAiClient client) {
            super(null, null, null, processor, client, null, new EarningsProperties(), null, null, Runnable::run);
        }
        FinancialAnalysis call(Filing filing, List<FilingDocument> docs) { return analyzeContent(filing, docs); }
    }
}

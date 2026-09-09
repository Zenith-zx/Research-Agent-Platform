package com.selfinvestor.earnings.analysis;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.enums.FundamentalTrend;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.filing.FilingContentProcessor;
import com.selfinvestor.earnings.filing.FilingDocumentPO;
import com.selfinvestor.earnings.service.AiAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class AiAnalysisPOServiceTest {
    /**
     * 验证解析文档会被发送给 OpenAI 并返回结构化结果。
     */
    @Test
    void sendsParsedDocumentToOpenAiAndReturnsStructuredResult() {
        EarningsProperties properties = new EarningsProperties();
        properties.getOpenai().setMaxInputChars(60000);
        OpenAiClient openAi = mock(OpenAiClient.class);
        FinancialAnalysisDTO expected = new FinancialAnalysisDTO("NVIDIA", "NVDA", "Q2", "Growth", null, null,
                Collections.emptyList(), Collections.singletonList("Revenue growth"), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), FundamentalTrend.IMPROVING,
                "Growth accelerated", false, null, false, "当前版本没有接入分析师一致预期，因此无法判断 Beat/Miss。", 8, "Strong quarter");
        when(openAi.analyze(contains("Revenue was $30 billion"))).thenReturn(expected);
        CompanyPO company = new CompanyPO("NVDA", "0001045810", "NVIDIA", true);
        FilingPO filing = new FilingPO(company, "0001-24-000001", "10-Q", LocalDate.now(), Instant.now(), "q.htm", "url");
        FilingDocumentPO document = new FilingDocumentPO(filing, "10-Q", "q.htm", "Quarterly report", "url", "raw");
        document.parsed("Revenue was $30 billion", true);

        FilingContentProcessor processor = new FilingContentProcessor();
        ReflectionTestUtils.setField(processor, "properties", properties);
        ReflectionTestUtils.invokeMethod(processor, "initialize");
        TestableService service = new TestableService();
        ReflectionTestUtils.setField(service, "processor", processor);
        ReflectionTestUtils.setField(service, "client", openAi);
        ReflectionTestUtils.setField(service, "mapper", new com.fasterxml.jackson.databind.ObjectMapper());
        ReflectionTestUtils.setField(service, "analysisExecutor", (java.util.concurrent.Executor) Runnable::run);
        FinancialAnalysisDTO result = service.call(filing, Collections.singletonList(document));
        assertThat(result).isSameAs(expected);
        verify(openAi).analyze(contains("Ticker: NVDA"));
    }

    private static class TestableService extends AiAnalysisService {
        /**
         * 暴露待测试的内容分析方法。
         *
         * @param filing Filing 实体
         * @param docs 文档列表
         * @return 结构化分析结果
         */
        FinancialAnalysisDTO call(FilingPO filing, List<FilingDocumentPO> docs) {
            return analyzeContent(filing, docs);
        }
    }
}

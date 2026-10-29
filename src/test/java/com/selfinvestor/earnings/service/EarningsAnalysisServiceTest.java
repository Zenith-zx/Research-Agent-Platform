package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentService;
import com.selfinvestor.earnings.config.EarningsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class EarningsAnalysisServiceTest {

    @Test
    void chainModeDelegatesToUnchangedLegacyService() {
        AiAnalysisService legacy = mock(AiAnalysisService.class);
        EarningsAgentService agent = mock(EarningsAgentService.class);
        EarningsProperties properties = new EarningsProperties();
        properties.setAnalysisMode(EarningsProperties.AnalysisMode.CHAIN);
        EarningsAnalysisService service = new EarningsAnalysisService();
        ReflectionTestUtils.setField(service, "legacyAnalysis", legacy);
        ReflectionTestUtils.setField(service, "agent", agent);
        ReflectionTestUtils.setField(service, "properties", properties);

        service.analyze(42L);

        verify(legacy).analyze(42L);
        verifyNoInteractions(agent);
    }
}

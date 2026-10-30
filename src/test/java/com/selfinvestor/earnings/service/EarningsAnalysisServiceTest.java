package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentService;
import com.selfinvestor.earnings.agent.runtime.submission.AgentSubmissionService;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
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
        ReflectionTestUtils.setField(service, "runtimeProperties", new AgentRuntimeProperties());

        service.analyze(42L);

        verify(legacy).analyze(42L);
        verifyNoInteractions(agent);
    }

    @Test
    void enabledRuntimeQueuesEarningsSubmission() {
        AiAnalysisService legacy = mock(AiAnalysisService.class);
        EarningsAgentService graph = mock(EarningsAgentService.class);
        AgentSubmissionService submissions = mock(AgentSubmissionService.class);
        AgentRuntimeProperties runtime = new AgentRuntimeProperties();
        runtime.setEnabled(true);
        EarningsAnalysisService service = new EarningsAnalysisService();
        ReflectionTestUtils.setField(service, "legacyAnalysis", legacy);
        ReflectionTestUtils.setField(service, "agent", graph);
        ReflectionTestUtils.setField(service, "runtimeProperties", runtime);
        ReflectionTestUtils.setField(service, "submissions", submissions);

        service.analyze(84L);

        verify(submissions).submitEarnings(84L);
        verifyNoInteractions(legacy, graph);
    }
}

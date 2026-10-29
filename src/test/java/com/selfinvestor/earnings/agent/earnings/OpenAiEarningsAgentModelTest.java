package com.selfinvestor.earnings.agent.earnings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.agent.earnings.graph.AgentNodeName;
import com.selfinvestor.earnings.agent.earnings.model.PlannerOutput;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsPromptService;
import com.selfinvestor.earnings.agent.earnings.prompt.OpenAiEarningsAgentModel;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import com.selfinvestor.earnings.config.EarningsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpenAiEarningsAgentModelTest {

    @Test
    void retriesInvalidJsonAndCountsEachLlmCall() {
        OpenAiClient client = mock(OpenAiClient.class);
        EarningsPromptService prompts = prompts();
        EarningsProperties properties = new EarningsProperties();
        properties.getEarningsAgent().setJsonRetryAttempts(2);
        PlannerOutput valid = new PlannerOutput();
        when(client.generateStructured(anyString(), anyString(), any(), anyString(), eq(PlannerOutput.class)))
                .thenThrow(new IllegalArgumentException("invalid JSON"))
                .thenReturn(valid);
        EarningsAgentState state = state();

        PlannerOutput result = model(client, prompts, properties).plan(state);

        assertThat(result).isSameAs(valid);
        assertThat(state.getLlmCallCount()).isEqualTo(2);
        verify(client, times(2)).generateStructured(anyString(), anyString(), any(), anyString(), eq(PlannerOutput.class));
    }

    @Test
    void failsSafelyAfterJsonRetriesAreExhausted() {
        OpenAiClient client = mock(OpenAiClient.class);
        EarningsProperties properties = new EarningsProperties();
        properties.getEarningsAgent().setJsonRetryAttempts(1);
        when(client.generateStructured(anyString(), anyString(), any(), anyString(), eq(PlannerOutput.class)))
                .thenThrow(new IllegalArgumentException("invalid JSON"));
        EarningsAgentState state = state();
        OpenAiEarningsAgentModel model = model(client, prompts(), properties);

        assertThatThrownBy(() -> model.plan(state))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid JSON");
        assertThat(state.getLlmCallCount()).isEqualTo(2);
    }

    private EarningsPromptService prompts() {
        EarningsPromptService prompts = mock(EarningsPromptService.class);
        when(prompts.prompt(AgentNodeName.PLANNER)).thenReturn("planner");
        ObjectNode schema = new ObjectMapper().createObjectNode().put("type", "object");
        when(prompts.schema(AgentNodeName.PLANNER)).thenReturn(schema);
        return prompts;
    }

    private OpenAiEarningsAgentModel model(OpenAiClient client, EarningsPromptService prompts,
                                           EarningsProperties properties) {
        OpenAiEarningsAgentModel model = new OpenAiEarningsAgentModel();
        ReflectionTestUtils.setField(model, "client", client);
        ReflectionTestUtils.setField(model, "prompts", prompts);
        ReflectionTestUtils.setField(model, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(model, "properties", properties);
        return model;
    }

    private EarningsAgentState state() {
        EarningsEvent event = new EarningsEvent("NVDA", "NVIDIA", 1L, "a", "10-Q", LocalDate.now(), "url", "filing");
        return EarningsAgentState.initialize("NVDA", "NVIDIA", event, 3, 20, 15);
    }
}

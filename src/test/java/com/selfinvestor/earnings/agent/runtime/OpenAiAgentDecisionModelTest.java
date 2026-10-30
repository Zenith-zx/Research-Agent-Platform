package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.decision.DecisionType;
import com.selfinvestor.earnings.agent.runtime.profile.AgentProfile;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpenAiAgentDecisionModelTest {
    @Test
    void usesSelectedProfileAndConvertsStrictWireArgumentsWithoutLosingStructuredResult() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        OpenAiClient client = mock(OpenAiClient.class);
        OpenAiAgentDecisionModel model = new OpenAiAgentDecisionModel();
        ReflectionTestUtils.setField(model, "client", client);
        ReflectionTestUtils.setField(model, "mapper", mapper);
        var schema = mapper.readTree("{\"type\":\"object\"}");
        AgentContext context = new AgentContext("input");
        context.setProfile(new AgentProfile("US_MACRO_ANALYST", "macro instructions",
                Set.of("get_macro_history"), 15, 15, schema));
        var wire = new OpenAiAgentDecisionModel.WireDecision();
        wire.setType(DecisionType.TOOL_CALL);
        wire.setToolName("get_macro_history");
        wire.setArgumentsJson("{\"indicator\":\"CPI\",\"limit\":12}");
        wire.setStructuredResult(mapper.readTree("{\"summary\":\"test fixture\"}"));
        when(client.generateStructured(eq("input"), eq("macro instructions"), same(schema),
                eq("investment_agent_decision"), eq(OpenAiAgentDecisionModel.WireDecision.class))).thenReturn(wire);

        var decision = model.decide(context);

        assertThat(decision.getArguments()).containsEntry("indicator", "CPI").containsEntry("limit", 12);
        assertThat(decision.getStructuredResult()).isEqualTo(wire.getStructuredResult());
        verify(client).generateStructured(eq("input"), eq("macro instructions"), same(schema),
                eq("investment_agent_decision"), eq(OpenAiAgentDecisionModel.WireDecision.class));
    }

    @Test
    void rejectsNonObjectToolArguments() {
        OpenAiClient client = mock(OpenAiClient.class);
        OpenAiAgentDecisionModel model = new OpenAiAgentDecisionModel();
        ReflectionTestUtils.setField(model, "client", client);
        ReflectionTestUtils.setField(model, "mapper", new ObjectMapper());
        AgentContext context = new AgentContext("input");
        context.setProfile(new AgentProfile("TEST", "test", Set.of(), 15, 15, null));
        var wire = new OpenAiAgentDecisionModel.WireDecision();
        wire.setArgumentsJson("[1,2]");
        when(client.generateStructured(anyString(), anyString(), isNull(), anyString(),
                eq(OpenAiAgentDecisionModel.WireDecision.class))).thenReturn(wire);

        assertThatThrownBy(() -> model.decide(context)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON object");
    }
}

package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.agent.runtime.decision.DecisionType;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.submission.SubmissionType;
import com.selfinvestor.earnings.agent.runtime.submission.TextTurnInput;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.tool.ToolExecutor;
import com.selfinvestor.earnings.agent.runtime.tool.ToolResult;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.agent.runtime.turn.TurnManager;
import com.selfinvestor.earnings.agent.runtime.turn.TurnStatus;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentLoopTest {
    private AgentContextBuilder contextBuilder;
    private AgentDecisionModel model;
    private ToolExecutor toolExecutor;
    private TurnItemService items;
    private TurnManager turns;
    private AgentRuntimeProperties properties;
    private AgentLoop loop;

    @BeforeEach
    void setUp() {
        contextBuilder = mock(AgentContextBuilder.class);
        model = mock(AgentDecisionModel.class);
        toolExecutor = mock(ToolExecutor.class);
        items = mock(TurnItemService.class);
        turns = mock(TurnManager.class);
        properties = new AgentRuntimeProperties();
        loop = new AgentLoop();
        ReflectionTestUtils.setField(loop, "contextBuilder", contextBuilder);
        ReflectionTestUtils.setField(loop, "decisionModel", model);
        ReflectionTestUtils.setField(loop, "toolExecutor", toolExecutor);
        ReflectionTestUtils.setField(loop, "items", items);
        ReflectionTestUtils.setField(loop, "turns", turns);
        ReflectionTestUtils.setField(loop, "properties", properties);
        ReflectionTestUtils.setField(loop, "mapper", new ObjectMapper());
        AgentCompletionService completion = mock(AgentCompletionService.class);
        ReflectionTestUtils.setField(loop, "completion", completion);
        when(completion.finish(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(invocation -> {
            AgentTurn turn = invocation.getArgument(1);
            AgentDecision decision = invocation.getArgument(2);
            turn.complete(decision.getFinalAnswer());
            return true;
        });
        when(contextBuilder.build(any(), any())).thenReturn(new AgentContext("context"));
        when(toolExecutor.execute(any(), anyString(), anyMap())).thenReturn(ToolResult.success(Map.of("ok", true), 1));
    }

    @Test
    void toolCallsFeedBackIntoModelUntilFinalAnswer() {
        when(model.decide(any()))
                .thenReturn(tool("get_company_information", Map.of("ticker", "NVDA")))
                .thenReturn(tool("get_historical_filings", Map.of("ticker", "NVDA")))
                .thenReturn(finalAnswer("analysis complete"));
        Scenario scenario = scenario(12, 12);

        AgentTurn result = loop.execute(scenario.thread(), scenario.turn(), scenario.submission(), new AtomicBoolean());

        assertThat(result.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(result.getFinalResponse()).isEqualTo("analysis complete");
        assertThat(result.getStepCount()).isEqualTo(3);
        verify(toolExecutor, times(2)).execute(any(), anyString(), anyMap());
    }

    @Test
    void maxStepsTerminatesTurn() {
        when(model.decide(any())).thenReturn(tool("get_company_information", Map.of()));
        Scenario scenario = scenario(1, 12);

        AgentTurn result = loop.execute(scenario.thread(), scenario.turn(), scenario.submission(), new AtomicBoolean());

        assertThat(result.getStatus()).isEqualTo(TurnStatus.FAILED);
        assertThat(result.getFailureMessage()).startsWith("LIMIT_REACHED");
        verify(model, times(1)).decide(any());
    }

    @Test
    void repeatedToolCallIsNotExecutedPastConfiguredLimit() {
        properties.setMaxRepeatedToolCalls(1);
        AgentDecision repeated = tool("get_company_information", Map.of("ticker", "NVDA"));
        when(model.decide(any()))
                .thenReturn(repeated)
                .thenReturn(repeated)
                .thenReturn(finalAnswer("stopped repeating"));
        Scenario scenario = scenario(12, 12);

        AgentTurn result = loop.execute(scenario.thread(), scenario.turn(), scenario.submission(), new AtomicBoolean());

        assertThat(result.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        verify(toolExecutor, times(1)).execute(any(), anyString(), anyMap());
    }

    private Scenario scenario(int maxSteps, int maxLlmCalls) {
        AgentThread thread = AgentThread.create(SubjectType.STOCK, "NVDA");
        AgentTurn turn = AgentTurn.create(thread.getId(), SubmissionType.USER_INPUT.name(), "research",
                maxSteps, maxLlmCalls);
        turn.start();
        Submission submission = Submission.builder()
                .threadId(thread.getId())
                .type(SubmissionType.USER_INPUT)
                .goal("research")
                .input(new TextTurnInput("research NVDA"))
                .build();
        return new Scenario(thread, turn, submission);
    }

    private AgentDecision tool(String name, Map<String, Object> arguments) {
        AgentDecision decision = new AgentDecision();
        decision.setType(DecisionType.TOOL_CALL);
        decision.setReasoningSummary("need data");
        decision.setToolName(name);
        decision.setArguments(new LinkedHashMap<>(arguments));
        return decision;
    }

    private AgentDecision finalAnswer(String answer) {
        AgentDecision decision = new AgentDecision();
        decision.setType(DecisionType.FINAL_ANSWER);
        decision.setReasoningSummary("enough evidence");
        decision.setFinalAnswer(answer);
        return decision;
    }

    private record Scenario(AgentThread thread, AgentTurn turn, Submission submission) {
    }
}

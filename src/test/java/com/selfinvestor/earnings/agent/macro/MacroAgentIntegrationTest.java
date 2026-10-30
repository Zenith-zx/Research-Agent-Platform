package com.selfinvestor.earnings.agent.macro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.macro.model.*;
import com.selfinvestor.earnings.agent.runtime.*;
import com.selfinvestor.earnings.agent.runtime.decision.*;
import com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles;
import com.selfinvestor.earnings.agent.runtime.submission.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.agent.runtime.tool.*;
import com.selfinvestor.earnings.agent.runtime.turn.*;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import com.selfinvestor.earnings.marketstress.provider.MarketDataProvider;
import com.selfinvestor.earnings.marketstress.provider.MarketNewsProvider;
import com.selfinvestor.earnings.repository.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:macro;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "self-investor.agent.enabled=false", "self-investor.macro.enabled=true"})
@Transactional
class MacroAgentIntegrationTest {
    @Resource private MacroTrigger trigger;
    @Resource private MacroReleaseService releases;
    @Resource private MacroStateService states;
    @Resource private MacroRepository repository;
    @Resource private AgentRuntime runtime;
    @Resource private AgentRuntimeProperties limits;
    @Resource private AgentContextBuilder contexts;
    @Resource private AgentProfiles profiles;
    @Resource private ToolExecutor tools;
    @Resource private ThreadManager threads;
    @Resource private TurnManager turns;
    @Resource private TurnItemService items;
    @Resource private SubmissionQueue queue;
    @Resource private ObjectMapper mapper;
    @MockitoBean private AgentDecisionModel model;
    @MockitoBean private MarketDataProvider market;
    @MockitoBean private MarketNewsProvider news;
    private AgentThread thread;

    @BeforeEach
    void setUp() {
        // Dispatcher remains disabled by the startup property; exercise the same queue synchronously.
        limits.setEnabled(true);
        limits.setMaxRepeatedToolCalls(2);
        limits.setMaxToolFailures(5);
        while (queue.poll(Duration.ZERO) != null) {
        }
        thread = threads.getOrCreate(SubjectType.MACRO, "US_MACRO");
        states.getOrCreate(thread.getId());
    }

    @Test
    void cpiRunsDynamicHistoryYieldsDollarThenPersistsFinal() {
        MacroRelease release = release("CPI");
        releases.store(release);
        when(model.decide(any())).thenReturn(tool("get_macro_history", Map.of("indicator", "CPI")),
                tool("get_treasury_yields", Map.of()), tool("get_dollar_index", Map.of()), finalDecision());
        Submission submitted = trigger.published(SubmissionType.CPI_RELEASED, release);
        assertThat(states.getCurrentUsMacroState().getVersion()).isZero();
        verifyNoInteractions(model);
        assertThat(queue.poll(Duration.ZERO).getSubmissionId()).isEqualTo(submitted.getSubmissionId());

        AgentTurn turn = execute(submitted);

        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(turn.getMaxSteps()).isEqualTo(15);
        assertThat(turn.getStepCount()).isEqualTo(4);
        assertThat(calls(turn)).containsExactly("get_macro_history", "get_treasury_yields", "get_dollar_index");
        assertThat(repository.findAnalysis(turn.getId())).isPresent();
        assertThat(turn.getFinalResponse()).contains("What Changed", "Market Transmission", "Research Only");
        assertThat(threads.require(thread.getId()).getMemory()).contains("regimes", "recentChanges")
                .doesNotContain("Tool completed");
    }

    @Test
    void fomcSelectsStatementComparisonAndYields() {
        MacroRelease release = release("FOMC");
        release.setStatement("Inflation has made further progress.");
        release.setPreviousStatement("Inflation remains elevated.");
        release.setRateDecision("Unchanged; test fixture only");
        when(model.decide(any())).thenReturn(tool("get_fomc_statement", Map.of("releaseId", release.getReleaseId())),
                tool("compare_previous_fomc_statement", Map.of("releaseId", release.getReleaseId())),
                tool("get_treasury_yields", Map.of()), finalDecision());
        Submission submission = trigger.published(SubmissionType.FOMC_RELEASED, release);

        AgentTurn turn = execute(submission);

        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(calls(turn)).containsExactly("get_fomc_statement", "compare_previous_fomc_statement", "get_treasury_yields");
        assertThat(items.history(turn.getId()).stream().filter(item -> item.getType() == TurnItemType.TOOL_RESULT)
                .map(AgentTurnItem::getPayloadJson)).anyMatch(json -> json.contains("previousStatement")
                        && json.contains("Inflation remains elevated."));
    }

    @Test
    void stickyToReacceleratingPersistsOnlyChangedDimension() {
        states.apply(thread.getId(), inflation(InflationRegime.UNKNOWN, InflationRegime.STICKY));
        MacroState before = states.getCurrentUsMacroState();
        AgentDecision finalDecision = finalDecision();
        MacroAnalysisResult result = mapper.convertValue(finalDecision.getStructuredResult(), MacroAnalysisResult.class);
        result.setProposedStateChange(inflation(InflationRegime.STICKY, InflationRegime.REACCELERATING));
        finalDecision.setStructuredResult(mapper.valueToTree(result));
        when(model.decide(any())).thenReturn(finalDecision);

        AgentTurn turn = execute(trigger.input("Evaluate repeated inflation surprises", false));

        MacroState after = states.getCurrentUsMacroState();
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(after.getInflation()).isEqualTo(InflationRegime.REACCELERATING);
        assertThat(after.getVersion()).isEqualTo(before.getVersion() + 1);
        assertThat(after.getFed()).isEqualTo(FedRegime.UNKNOWN);
        assertThat(repository.findAnalysis(turn.getId()).orElseThrow().getChangesJson())
                .contains("STICKY", "REACCELERATING").doesNotContain("\"fed\"");
    }

    @Test
    void noChangeLeavesAllStateColumnsVersionAndTimestampUntouched() {
        MacroState before = states.getCurrentUsMacroState();
        when(model.decide(any())).thenReturn(finalDecision());
        AgentTurn turn = execute(trigger.input("No material change", false));
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
        assertThat(repository.findAnalysis(turn.getId()).orElseThrow().getChangesJson()).isEqualTo("{}");
    }

    @Test
    void noopAndStaleFromAreNotAllowedToOverwriteState() {
        MacroState before = states.getCurrentUsMacroState();
        states.apply(thread.getId(), inflation(InflationRegime.UNKNOWN, InflationRegime.UNKNOWN));
        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
        assertThatThrownBy(() -> states.apply(thread.getId(),
                inflation(InflationRegime.STICKY, InflationRegime.REACCELERATING)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Stale");
        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
    }

    @Test
    void newSteerDuringFinalDecisionForcesAnotherDecisionOnSameTurn() {
        Submission original = trigger.input("Analyze CPI", false);
        TurnSubmissionOutcome started = turns.startOrSteer(thread, original);
        AtomicInteger calls = new AtomicInteger();
        when(model.decide(any())).thenAnswer(invocation -> {
            if (calls.getAndIncrement() == 0) {
                TurnSubmissionOutcome outcome = turns.startOrSteer(thread, trigger.input("重点解释 QQQ reaction", true));
                assertThat(outcome.getResult()).isEqualTo(TurnSubmissionResult.STEERED);
                assertThat(outcome.getTurn().getId()).isEqualTo(started.getTurn().getId());
            } else {
                AgentContext context = invocation.getArgument(0);
                assertThat(context.getInput()).contains("QQQ reaction", "STEER_INPUT");
            }
            return finalDecision();
        });

        runtime.start(thread, started.getTurn(), original);

        assertThat(calls.get()).isEqualTo(2);
        assertThat(turns.require(started.getTurn().getId()).getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(items.history(started.getTurn().getId()).stream()
                .filter(item -> item.getType() == TurnItemType.FINAL_RESPONSE).count()).isEqualTo(1);
    }

    @Test
    void treasuryFailureIsObservedAndModelCanContinueToFinal() {
        when(market.getMetric(any())).thenThrow(new IllegalStateException("Treasury provider unavailable"));
        when(model.decide(any())).thenReturn(tool("get_treasury_yields", Map.of()),
                tool("get_macro_history", Map.of("indicator", "CPI")), finalDecision());

        AgentTurn turn = execute(trigger.input("Analyze with limited evidence", false));

        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(items.history(turn.getId()).stream().filter(item -> item.getType() == TurnItemType.TOOL_RESULT)
                .map(AgentTurnItem::getPayloadJson)).anyMatch(json -> json.contains("\"success\":false")
                        && json.contains("Treasury provider unavailable"));
    }

    @Test
    void duplicateToolCallDoesNotReachProviderBeyondLimit() {
        limits.setMaxRepeatedToolCalls(1);
        AgentDecision repeated = tool("get_dollar_index", Map.of());
        when(model.decide(any())).thenReturn(repeated, repeated, finalDecision());

        AgentTurn turn = execute(trigger.input("Do not repeat", false));

        assertThat(turn.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        verify(market, times(1)).getMetric(com.selfinvestor.earnings.enums.MarketMetricType.DXY);
        assertThat(items.history(turn.getId()).stream().map(AgentTurnItem::getContent))
                .anyMatch(content -> content != null && content.contains("Repeated tool call limit"));
    }

    @Test
    void waitingResumePreservesToolLimitsAndOriginalTurn() {
        limits.setMaxRepeatedToolCalls(1);
        AgentDecision wait = new AgentDecision();
        wait.setType(DecisionType.WAIT_FOR_USER);
        wait.setReasoningSummary("Please provide historical observations");
        when(model.decide(any())).thenReturn(tool("get_dollar_index", Map.of()), wait,
                tool("get_dollar_index", Map.of()), finalDecision());
        Submission initial = trigger.input("Analyze US macro", false);
        AgentTurn waiting = execute(initial);
        assertThat(waiting.getStatus()).isEqualTo(TurnStatus.WAITING);
        Submission steer = trigger.input("No more data; explain limitations", true);
        TurnSubmissionOutcome outcome = turns.startOrSteer(thread, steer);
        assertThat(outcome.getTurn().getId()).isEqualTo(waiting.getId());

        runtime.start(thread, outcome.getTurn(), steer);

        assertThat(turns.require(waiting.getId()).getStatus()).isEqualTo(TurnStatus.COMPLETED);
        verify(market, times(1)).getMetric(com.selfinvestor.earnings.enums.MarketMetricType.DXY);
    }

    @Test
    void profileHidesAndExecutorRejectsCompanyTools() throws Exception {
        Submission submission = trigger.input("Macro only", false);
        AgentTurn turn = turns.startOrSteer(thread, submission).getTurn();
        AgentContext context = contexts.build(thread, turn);
        JsonNode root = mapper.readTree(context.getInput());
        assertThat(root.path("domain").path("macroState").path("inflation").asText()).isEqualTo("UNKNOWN");
        assertThat(root.path("availableTools").toString()).contains("get_macro_release")
                .doesNotContain("get_company_information");
        ToolResult result = tools.execute(new AgentToolContext(thread, turn, submission), "get_company_information", Map.of());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("not allowed");
        assertStrictObjects(profiles.resolve(thread).getDecisionSchema());
    }

    @Test
    void nfpAndManualUserInputHaveDifferentGoalsAndPlaceholderTriggersAreRejected() {
        MacroRelease release = release("NFP");
        MacroRelease.Reading payroll = new MacroRelease.Reading();
        payroll.setUnit("thousand persons");
        payroll.setActual(new java.math.BigDecimal("100"));
        payroll.setRevision(new java.math.BigDecimal("-20"));
        release.setReadings(Map.of("payrolls", payroll));
        Submission nfp = trigger.published(SubmissionType.NFP_RELEASED, release);
        assertThat(nfp.getGoal()).contains("payroll", "participation", "revisions");
        assertThat(releases.require(release.getReleaseId()).getReadings().get("payrolls").getRevision())
                .isEqualByComparingTo("-20");
        assertThat(trigger.input("Explain growth", false).getType()).isEqualTo(SubmissionType.MACRO_USER_INPUT);
        assertThatThrownBy(() -> trigger.published(SubmissionType.PCE_RELEASED, release))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importedHistoryIsImmutableAndMissingComponentsAreExplicit() {
        MacroRelease release = release("CPI");
        releases.store(release);
        releases.store(release);
        Submission submission = trigger.input("Investigate CPI", false);
        AgentTurn turn = turns.startOrSteer(thread, submission).getTurn();
        ToolResult result = tools.execute(new AgentToolContext(thread, turn, submission), "get_cpi_components",
                Map.of("releaseId", release.getReleaseId()));
        assertThat(mapper.valueToTree(result.getData()).path("available").asBoolean()).isFalse();
        release.setSource("A different source");
        assertThatThrownBy(() -> releases.store(release)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repeatedProviderFailuresReachHardLimitWithoutLooping() {
        limits.setMaxToolFailures(2);
        when(market.getMetric(any())).thenThrow(new IllegalStateException("No provider"));
        when(model.decide(any())).thenReturn(tool("get_treasury_yields", Map.of()),
                tool("get_dollar_index", Map.of()), finalDecision());
        AgentTurn turn = execute(trigger.input("Failure budget", false));
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.FAILED);
        assertThat(turn.getFailureMessage()).contains("TOOL_FAILURE_LIMIT_REACHED");
        verify(model, times(2)).decide(any());
        assertThat(repository.findAnalysis(turn.getId())).isEmpty();
    }

    @Test
    void invalidEnumAndUnknownStateDimensionsCannotPersist() {
        AgentDecision decision = finalDecision();
        var change = mapper.createObjectNode();
        change.putObject("inflation").put("from", "UNKNOWN").put("to", "MAGIC").put("reason", "Invalid");
        ((com.fasterxml.jackson.databind.node.ObjectNode) decision.getStructuredResult())
                .set("proposedStateChange", change);
        when(model.decide(any())).thenReturn(decision);
        MacroState before = states.getCurrentUsMacroState();
        AgentTurn turn = execute(trigger.input("Reject invalid state", false));
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.FAILED);
        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
        assertThat(repository.findAnalysis(turn.getId())).isEmpty();
    }

    private AgentTurn execute(Submission submission) {
        TurnSubmissionOutcome outcome = turns.startOrSteer(thread, submission);
        runtime.start(thread, outcome.getTurn(), submission);
        return turns.require(outcome.getTurn().getId());
    }

    private List<String> calls(AgentTurn turn) {
        return items.history(turn.getId()).stream().filter(item -> item.getType() == TurnItemType.TOOL_CALL)
                .map(AgentTurnItem::getToolName).toList();
    }

    private MacroRelease release(String indicator) {
        MacroRelease release = new MacroRelease();
        release.setReleaseId("fixture-" + UUID.randomUUID());
        release.setIndicator(indicator);
        release.setPeriod("2025-01");
        release.setReleaseDate(OffsetDateTime.parse("2025-02-12T13:30:00Z"));
        release.setSource("TEST FIXTURE - not real economic data");
        return release;
    }

    private AgentDecision tool(String name, Map<String, Object> args) {
        AgentDecision decision = new AgentDecision();
        decision.setType(DecisionType.TOOL_CALL);
        decision.setToolName(name);
        decision.setArguments(args);
        decision.setReasoningSummary("Need evidence: " + name);
        return decision;
    }

    private AgentDecision finalDecision() {
        MacroAnalysisResult result = new MacroAnalysisResult();
        result.setSummary("Test research: insufficient evidence for a regime change");
        result.setWhatChanged("Consensus and trend coverage are incomplete.");
        result.setProposedStateChange(new MacroStateChange());
        result.setKeyFindings(List.of("Reported data is distinct from market reaction."));
        result.setMarketImpacts(List.of("Fed expectations can affect discount rates; causality is uncertain."));
        result.setMarketReaction("No event-window market evidence.");
        result.setResearchImplications(List.of("Monitor subsequent releases, not a trading recommendation."));
        result.setRisks(List.of("Data revisions, pricing and observation gaps."));
        result.setConfidence(ConfidenceLevel.LOW);
        AgentDecision decision = new AgentDecision();
        decision.setType(DecisionType.FINAL_ANSWER);
        decision.setReasoningSummary("Enough to explain limitations");
        decision.setFinalAnswer(result.getSummary());
        decision.setStructuredResult(mapper.valueToTree(result));
        return decision;
    }

    private MacroStateChange inflation(InflationRegime from, InflationRegime to) {
        RegimeChange<InflationRegime> change = new RegimeChange<>();
        change.setFrom(from);
        change.setTo(to);
        change.setReason("Test fixture: multiple consistent observations, not a real macro assertion");
        MacroStateChange proposed = new MacroStateChange();
        proposed.setInflation(change);
        return proposed;
    }

    private void assertStrictObjects(JsonNode node) {
        if (node.isObject() && node.path("type").asText().equals("object")) {
            assertThat(node.path("additionalProperties").asBoolean(true)).isFalse();
            Set<String> fields = new HashSet<>();
            node.path("properties").fieldNames().forEachRemaining(fields::add);
            Set<String> required = new HashSet<>();
            node.path("required").forEach(value -> required.add(value.asText()));
            assertThat(required).isEqualTo(fields);
        }
        node.elements().forEachRemaining(this::assertStrictObjects);
    }
}

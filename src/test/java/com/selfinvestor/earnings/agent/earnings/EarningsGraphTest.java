package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.graph.*;
import com.selfinvestor.earnings.agent.earnings.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsGraphTest {

    @Test
    void sufficientEvidenceSkipsReplanner() {
        List<AgentNodeName> visited = new ArrayList<>();
        EarningsGraph graph = graph(visited, state -> sufficient(state), state -> {
            throw new AssertionError("Replanner must not execute");
        });

        EarningsAgentState result = graph.execute(state(3));

        assertThat(visited).containsExactly(AgentNodeName.PLANNER, AgentNodeName.RESEARCH,
                AgentNodeName.CRITIC, AgentNodeName.FINALIZER);
        assertThat(result.getStatus()).isEqualTo(AgentStatus.COMPLETED);
    }

    @Test
    void insufficientEvidenceLoopsThroughReplanner() {
        List<AgentNodeName> visited = new ArrayList<>();
        AtomicInteger criticCalls = new AtomicInteger();
        EarningsGraph graph = graph(visited, state -> {
            if (criticCalls.getAndIncrement() == 0) {
                state.setCriticResult(critic(CriticDecision.NEED_MORE_RESEARCH));
            } else {
                sufficient(state);
            }
        }, state -> state.setCurrentRound(state.getCurrentRound() + 1));

        EarningsAgentState result = graph.execute(state(3));

        assertThat(visited).containsExactly(AgentNodeName.PLANNER, AgentNodeName.RESEARCH,
                AgentNodeName.CRITIC, AgentNodeName.REPLANNER, AgentNodeName.RESEARCH,
                AgentNodeName.CRITIC, AgentNodeName.FINALIZER);
        assertThat(result.getCurrentRound()).isEqualTo(2);
    }

    @Test
    void repeatedInsufficientEvidenceStopsAtMaxRounds() {
        List<AgentNodeName> visited = new ArrayList<>();
        EarningsGraph graph = graph(visited,
                state -> state.setCriticResult(critic(CriticDecision.NEED_MORE_RESEARCH)),
                state -> state.setCurrentRound(state.getCurrentRound() + 1));

        EarningsAgentState result = graph.execute(state(2));

        assertThat(visited).containsExactly(AgentNodeName.PLANNER, AgentNodeName.RESEARCH,
                AgentNodeName.CRITIC, AgentNodeName.REPLANNER, AgentNodeName.RESEARCH,
                AgentNodeName.CRITIC, AgentNodeName.FINALIZER);
        assertThat(result.getCriticResult().getDecision()).isEqualTo(CriticDecision.BEST_EFFORT_FINISH);
        assertThat(result.getStatus()).isEqualTo(AgentStatus.BEST_EFFORT);
        assertThat(result.getStepCount()).isEqualTo(7);
    }

    private EarningsGraph graph(List<AgentNodeName> visited, Consumer<EarningsAgentState> critic,
                                Consumer<EarningsAgentState> replanner) {
        List<EarningsAgentNode> nodes = List.of(
                node(AgentNodeName.PLANNER, visited, state -> { }),
                node(AgentNodeName.RESEARCH, visited, state -> { }),
                node(AgentNodeName.CRITIC, visited, critic),
                node(AgentNodeName.REPLANNER, visited, replanner),
                node(AgentNodeName.FINALIZER, visited, this::finish));
        EarningsGraph graph = new EarningsGraph();
        ReflectionTestUtils.setField(graph, "nodeList", nodes);
        ReflectionTestUtils.setField(graph, "router", new EarningsGraphRouter());
        ReflectionTestUtils.setField(graph, "checkpoints", new NoopCheckpointStore());
        ReflectionTestUtils.invokeMethod(graph, "initialize");
        return graph;
    }

    private EarningsAgentNode node(AgentNodeName name, List<AgentNodeName> visited,
                                   Consumer<EarningsAgentState> action) {
        return new EarningsAgentNode() {
            @Override
            public AgentNodeName name() {
                return name;
            }

            @Override
            public void execute(EarningsAgentState state) {
                visited.add(name);
                action.accept(state);
            }
        };
    }

    private void finish(EarningsAgentState state) {
        EarningsConclusion conclusion = new EarningsConclusion();
        conclusion.setExecutiveSummary("done");
        conclusion.setConfidence(0.8);
        state.setConclusion(conclusion);
        boolean bestEffort = state.getCriticResult().getDecision() == CriticDecision.BEST_EFFORT_FINISH;
        state.setStatus(bestEffort ? AgentStatus.BEST_EFFORT : AgentStatus.COMPLETED);
    }

    private static void sufficient(EarningsAgentState state) {
        state.setCriticResult(critic(CriticDecision.SUFFICIENT));
    }

    private static CriticResult critic(CriticDecision decision) {
        CriticResult result = new CriticResult();
        result.setDecision(decision);
        result.setConfidence(0.8);
        return result;
    }

    private static EarningsAgentState state(int maxRounds) {
        EarningsEvent event = new EarningsEvent("NVDA", "NVIDIA", 1L, "accession", "10-Q",
                LocalDate.of(2026, 8, 1), "https://sec.example", "filing");
        return EarningsAgentState.initialize("NVDA", "NVIDIA", event, maxRounds, 20, 15);
    }

    private static class NoopCheckpointStore implements AgentCheckpointStore {
        @Override
        public void create(EarningsAgentState state) {
        }

        @Override
        public void save(EarningsAgentState state) {
        }
    }
}

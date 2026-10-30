package com.selfinvestor.earnings.agent.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.macro.model.*;
import com.selfinvestor.earnings.agent.runtime.AgentCompletionService;
import com.selfinvestor.earnings.agent.runtime.decision.*;
import com.selfinvestor.earnings.agent.runtime.submission.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.agent.runtime.turn.*;
import com.selfinvestor.earnings.repository.MacroRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:macroAtomic;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "self-investor.agent.enabled=false"})
class MacroCompletionTransactionTest {
    @Resource private ThreadManager threads;
    @Resource private TurnManager turns;
    @Resource private TurnItemService items;
    @Resource private MacroStateService states;
    @Resource private MacroRepository repository;
    @Resource private AgentCompletionService completion;
    @Resource private ObjectMapper mapper;

    @Test
    void failureAfterStateAndAnalysisWritesRollsBackEntireCompletionThenCancellationWins() {
        AgentThread thread = threads.getOrCreate(SubjectType.MACRO, "US_MACRO");
        MacroState before = states.getOrCreate(thread.getId());
        Submission submission = Submission.builder().threadId(thread.getId()).type(SubmissionType.MACRO_USER_INPUT)
                .goal("Transaction rollback fixture").input(new TextTurnInput("Test only")).build();
        AgentTurn turn = turns.startOrSteer(thread, submission).getTurn();
        int inputSequence = items.latestInputSequence(turn.getId());
        MacroAnalysisResult result = new MacroAnalysisResult();
        result.setSummary("Fixture only");
        result.setWhatChanged("Not real macro evidence");
        RegimeChange<InflationRegime> inflation = new RegimeChange<>();
        inflation.setFrom(InflationRegime.UNKNOWN);
        inflation.setTo(InflationRegime.STICKY);
        inflation.setReason("Test transaction rollback");
        MacroStateChange change = new MacroStateChange();
        change.setInflation(inflation);
        result.setProposedStateChange(change);
        // Deliberately invalid list is detected while rendering, after state/analysis/memory SQL.
        result.setKeyFindings(Arrays.asList("fixture", null));
        result.setMarketImpacts(List.of());
        result.setMarketReaction("Unknown");
        result.setResearchImplications(List.of());
        result.setRisks(List.of("Fixture"));
        result.setConfidence(ConfidenceLevel.LOW);
        AgentDecision decision = new AgentDecision();
        decision.setType(DecisionType.FINAL_ANSWER);
        decision.setFinalAnswer("fixture");
        decision.setStructuredResult(mapper.valueToTree(result));

        assertThatThrownBy(() -> completion.finish(thread, turn, decision, inputSequence))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("null");

        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
        assertThat(repository.findAnalysis(turn.getId())).isEmpty();
        assertThat(threads.require(thread.getId()).getMemory()).isEqualTo(thread.getMemory());
        assertThat(turns.require(turn.getId()).getStatus()).isEqualTo(TurnStatus.RUNNING);
        assertThat(items.history(turn.getId())).noneMatch(item -> item.getType() == TurnItemType.FINAL_RESPONSE);

        Submission cancel = Submission.builder().threadId(thread.getId()).type(SubmissionType.CANCEL).build();
        turns.startOrSteer(thread, cancel);
        assertThat(completion.finish(thread, turn, decision, inputSequence)).isTrue();
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.CANCELLED);
        assertThat(repository.findAnalysis(turn.getId())).isEmpty();
        assertThat(states.getCurrentUsMacroState()).isEqualTo(before);
    }
}

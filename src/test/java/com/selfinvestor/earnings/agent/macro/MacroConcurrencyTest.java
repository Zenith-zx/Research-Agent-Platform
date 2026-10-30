package com.selfinvestor.earnings.agent.macro;

import com.selfinvestor.earnings.agent.runtime.AgentDecisionModel;
import com.selfinvestor.earnings.agent.runtime.AgentRuntime;
import com.selfinvestor.earnings.agent.runtime.decision.*;
import com.selfinvestor.earnings.agent.runtime.submission.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.agent.runtime.turn.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:macroConcurrent;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "self-investor.agent.enabled=false"})
class MacroConcurrencyTest {
    @Resource private ThreadManager threads;
    @Resource private TurnManager turns;
    @Resource private AgentRuntime runtime;
    @MockitoBean private AgentDecisionModel model;

    @Test
    void concurrentEventsShareOneActiveTurnAndCancelledLoopExitsBeforeNextLoopStarts() throws Exception {
        AgentThread thread = threads.getOrCreate(SubjectType.MACRO, "US_MACRO");
        ExecutorService workers = Executors.newFixedThreadPool(8);
        CountDownLatch firstModelEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstModel = new CountDownLatch(1);
        CountDownLatch secondModelEntered = new CountDownLatch(1);
        try {
            List<Submission> inputs = new ArrayList<>();
            List<Future<TurnSubmissionOutcome>> starts = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                Submission submission = submission(thread, SubmissionType.MACRO_USER_INPUT);
                inputs.add(submission);
                starts.add(workers.submit(() -> turns.startOrSteer(thread, submission)));
            }
            List<TurnSubmissionOutcome> outcomes = new ArrayList<>();
            for (Future<TurnSubmissionOutcome> start : starts) {
                outcomes.add(start.get(5, TimeUnit.SECONDS));
            }
            assertThat(outcomes.stream().filter(value -> value.getResult() == TurnSubmissionResult.STARTED).count())
                    .isEqualTo(1);
            assertThat(outcomes.stream().map(value -> value.getTurn().getId()).distinct().count()).isEqualTo(1);
            AgentTurn first = outcomes.get(0).getTurn();
            AtomicInteger calls = new AtomicInteger();
            when(model.decide(any())).thenAnswer(invocation -> {
                if (calls.getAndIncrement() == 0) {
                    firstModelEntered.countDown();
                    assertThat(releaseFirstModel.await(5, TimeUnit.SECONDS)).isTrue();
                } else {
                    secondModelEntered.countDown();
                }
                AgentDecision decision = new AgentDecision();
                decision.setType(DecisionType.WAIT_FOR_USER);
                decision.setReasoningSummary("Fixture needs more evidence");
                return decision;
            });
            Future<?> running = workers.submit(() -> runtime.start(thread, first, inputs.get(0)));
            assertThat(firstModelEntered.await(5, TimeUnit.SECONDS)).isTrue();
            Submission cancel = submission(thread, SubmissionType.CANCEL);
            TurnSubmissionOutcome cancelled = turns.startOrSteer(thread, cancel);
            runtime.cancel(thread, cancelled.getTurn(), cancel);
            Submission nextInput = submission(thread, SubmissionType.MACRO_USER_INPUT);
            AgentTurn next = turns.startOrSteer(thread, nextInput).getTurn();
            assertThat(next.getId()).isNotEqualTo(first.getId());
            Future<?> nextRun = workers.submit(() -> runtime.start(thread, next, nextInput));
            assertThat(secondModelEntered.await(150, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirstModel.countDown();
            running.get(5, TimeUnit.SECONDS);
            nextRun.get(5, TimeUnit.SECONDS);
            assertThat(turns.require(first.getId()).getStatus()).isEqualTo(TurnStatus.CANCELLED);
            assertThat(turns.require(next.getId()).getStatus()).isEqualTo(TurnStatus.WAITING);
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            releaseFirstModel.countDown();
            workers.shutdownNow();
        }
    }

    private Submission submission(AgentThread thread, SubmissionType type) {
        return Submission.builder().threadId(thread.getId()).type(type)
                .goal("Concurrent fixture").input(new TextTurnInput("Concurrent test only")).build();
    }
}

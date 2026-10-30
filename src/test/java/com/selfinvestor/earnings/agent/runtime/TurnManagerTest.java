package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.submission.SubmissionType;
import com.selfinvestor.earnings.agent.runtime.submission.TextTurnInput;
import com.selfinvestor.earnings.agent.runtime.submission.TurnSubmissionOutcome;
import com.selfinvestor.earnings.agent.runtime.submission.TurnSubmissionResult;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.agent.runtime.turn.TurnManager;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import com.selfinvestor.earnings.repository.AgentTurnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TurnManagerTest {
    private AgentTurnRepository repository;
    private TurnItemService items;
    private TurnManager manager;
    private AgentRuntimeProperties properties;

    @BeforeEach
    void setUp() {
        repository = mock(AgentTurnRepository.class);
        items = mock(TurnItemService.class);
        properties = new AgentRuntimeProperties();
        manager = new TurnManager();
        ReflectionTestUtils.setField(manager, "repository", repository);
        ReflectionTestUtils.setField(manager, "items", items);
        ReflectionTestUtils.setField(manager, "properties", properties);
        ReflectionTestUtils.setField(manager, "mapper", new ObjectMapper());
        var profiles = mock(com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles.class);
        when(profiles.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(
                new com.selfinvestor.earnings.agent.runtime.profile.AgentProfile("TEST", "", java.util.Set.of(),
                        properties.getMaxSteps(), properties.getMaxLlmCalls(), null));
        ReflectionTestUtils.setField(manager, "profiles", profiles);
    }

    @Test
    void noActiveTurnStartsNewTurn() {
        AgentThread thread = AgentThread.create(SubjectType.STOCK, "NVDA");
        Submission submission = submission(thread, SubmissionType.USER_INPUT);
        when(repository.findActiveByThreadId(thread.getId())).thenReturn(Optional.empty());

        TurnSubmissionOutcome outcome = manager.startOrSteer(thread, submission);

        assertThat(outcome.getResult()).isEqualTo(TurnSubmissionResult.STARTED);
        assertThat(outcome.getTurn().getThreadId()).isEqualTo(thread.getId());
        assertThat(outcome.getTurn().getMaxSteps()).isEqualTo(properties.getMaxSteps());
        verify(repository).insert(outcome.getTurn());
    }

    @Test
    void activeTurnReceivesSteerWithoutCreatingAnotherTurn() {
        AgentThread thread = AgentThread.create(SubjectType.STOCK, "NVDA");
        AgentTurn active = AgentTurn.create(thread.getId(), SubmissionType.USER_INPUT.name(), "research", 12, 12);
        active.start();
        Submission submission = submission(thread, SubmissionType.STEER);
        when(repository.findActiveByThreadId(thread.getId())).thenReturn(Optional.of(active));

        TurnSubmissionOutcome outcome = manager.startOrSteer(thread, submission);

        assertThat(outcome.getResult()).isEqualTo(TurnSubmissionResult.STEERED);
        assertThat(outcome.getTurn()).isSameAs(active);
    }

    private Submission submission(AgentThread thread, SubmissionType type) {
        return Submission.builder()
                .threadId(thread.getId())
                .type(type)
                .goal("research")
                .input(new TextTurnInput("input"))
                .build();
    }
}

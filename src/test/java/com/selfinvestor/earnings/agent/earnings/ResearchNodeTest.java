package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.model.*;
import com.selfinvestor.earnings.agent.earnings.node.ResearchNode;
import com.selfinvestor.earnings.agent.earnings.prompt.EarningsAgentModel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResearchNodeTest {

    @Test
    void failedTaskIsRecordedForCriticInsteadOfCrashingGraph() {
        EarningsAgentModel model = mock(EarningsAgentModel.class);
        EarningsAgentState state = EarningsAgentState.initialize("NVDA", "NVIDIA",
                new EarningsEvent("NVDA", "NVIDIA", 1L, "a", "10-Q", LocalDate.now(), "url", "filing"),
                3, 20, 15);
        ResearchTask task = new ResearchTask("task-1", ResearchCategory.GUIDANCE, "What is guidance?",
                "Material", TaskPriority.HIGH, TaskStatus.PENDING);
        state.getResearchTasks().add(task);
        when(model.research(state, task)).thenThrow(new IllegalArgumentException("invalid JSON"));

        ResearchNode node = new ResearchNode();
        ReflectionTestUtils.setField(node, "model", model);
        node.execute(state);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(state.getOpenQuestions()).singleElement()
                .extracting(OpenQuestion::getQuestion).isEqualTo("What is guidance?");
    }
}

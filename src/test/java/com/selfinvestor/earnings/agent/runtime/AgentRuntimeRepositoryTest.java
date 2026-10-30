package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurnItem;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemType;
import com.selfinvestor.earnings.repository.AgentThreadRepository;
import com.selfinvestor.earnings.repository.AgentTurnItemRepository;
import com.selfinvestor.earnings.repository.AgentTurnRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AgentRuntimeRepositoryTest {
    @Resource
    private AgentThreadRepository threads;
    @Resource
    private AgentTurnRepository turns;
    @Resource
    private AgentTurnItemRepository items;

    @Test
    void threadTurnAndItemsRoundTripThroughMyBatis() {
        AgentThread thread = AgentThread.create(SubjectType.STOCK, "RUNTIME-TEST");
        threads.insert(thread);
        AgentTurn turn = AgentTurn.create(thread.getId(), "USER_INPUT", "research", 12, 12);
        turn.setSubmissionId("repository-test-submission");
        turn.setSubmissionMetadataJson("{}");
        turn.start();
        turns.insert(turn);
        AgentTurnItem item = AgentTurnItem.create(turn.getId(), TurnItemType.USER_INPUT,
                "analyze", null, null, null);
        item.setSequence(1);
        items.insert(item);

        assertThat(threads.findBySubject(SubjectType.STOCK, "RUNTIME-TEST")).isPresent();
        assertThat(turns.findActiveByThreadId(thread.getId())).isPresent();
        assertThat(items.findByTurnId(turn.getId())).extracting(AgentTurnItem::getContent).containsExactly("analyze");
        assertThat(items.findMaxSequence(turn.getId())).isEqualTo(1);
    }
}

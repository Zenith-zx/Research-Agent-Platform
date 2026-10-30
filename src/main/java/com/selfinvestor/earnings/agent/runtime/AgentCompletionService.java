package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 通用完成边界：领域状态、记忆、最终 Item 与 Turn COMPLETED 在同一事务内提交。
 */
@Service
public class AgentCompletionService {
    @Resource
    private AgentProfiles profiles;
    @Resource
    private TurnManager turns;
    @Resource
    private TurnItemService items;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 在 Thread 短锁内检查取消和新输入，再原子完成。
     * 模型调用期间新到达的 Steer 会令本次完成失效，由 Loop 重新读取后决策。
     */
    public boolean finish(AgentThread thread, AgentTurn turn, AgentDecision decision, int inputSequence) {
        return turns.locked(thread.getId(), () -> {
            AgentTurn stored = turns.require(turn.getId());
            if (stored.getStatus() == TurnStatus.CANCELLED) {
                turn.cancel();
                return true;
            }
            if (items.latestInputSequence(turn.getId()) > inputSequence) {
                return false;
            }
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                String answer = profiles.domain(thread)
                        .map(domain -> domain.complete(thread, turn, decision)).orElse(decision.getFinalAnswer());
                items.append(turn.getId(), TurnItemType.FINAL_RESPONSE, answer, null, null, json(decision));
                turn.complete(answer);
                turns.save(turn);
            });
            return true;
        });
    }

    /**
     * 将最终结构化决策保存到审计 Item，序列化失败使事务回滚。
     */
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize final decision", exception);
        }
    }
}

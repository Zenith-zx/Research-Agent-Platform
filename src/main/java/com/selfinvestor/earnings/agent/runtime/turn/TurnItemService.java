package com.selfinvestor.earnings.agent.runtime.turn;

import com.selfinvestor.earnings.repository.AgentTurnItemRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责按严格递增序号保存和读取 Turn Item。
 */
@Service
public class TurnItemService {
    /**
     * 最新外部输入序号；用于防止模型最终决策遗漏调用期间到达的输入。
     */
    public int latestInputSequence(String turnId) {
        return history(turnId).stream().filter(item -> item.getType() == TurnItemType.STEER_INPUT
                        || item.getType() == TurnItemType.USER_INPUT || item.getType() == TurnItemType.TRIGGER_INPUT)
                .mapToInt(AgentTurnItem::getSequence).max().orElse(0);
    }
    @Resource
    private AgentTurnItemRepository repository;

    private final ConcurrentHashMap<String, Object> turnLocks = new ConcurrentHashMap<>();

    /**
     * 向 Turn 追加一个事件。
     *
     * @param turnId Turn 标识
     * @param type 事件类型
     * @param content 可读内容
     * @param toolName Tool 名称
     * @param callId 调用标识
     * @param payloadJson JSON 负载
     * @return 保存后的 Item
     */
    public AgentTurnItem append(String turnId, TurnItemType type, String content,
                                String toolName, String callId, String payloadJson) {
        synchronized (turnLocks.computeIfAbsent(turnId, ignored -> new Object())) {
            AgentTurnItem item = AgentTurnItem.create(turnId, type, content, toolName, callId, payloadJson);
            item.setSequence(repository.findMaxSequence(turnId) + 1);
            repository.insert(item);
            return item;
        }
    }

    /**
     * 按事件顺序读取一个 Turn 的完整历史。
     *
     * @param turnId Turn 标识
     * @return Turn Item 列表
     */
    public List<AgentTurnItem> history(String turnId) {
        return repository.findByTurnId(turnId);
    }
}

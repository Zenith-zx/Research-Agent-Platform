package com.selfinvestor.earnings.agent.runtime.turn;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Turn 内一次输入、决策、Tool 调用或结果记录。
 */
@Data
@NoArgsConstructor
public class AgentTurnItem {
    private String id;
    private String turnId;
    private TurnItemType type;
    private String content;
    private String toolName;
    private String callId;
    private String payloadJson;
    private int sequence;
    private LocalDateTime createdAt;

    /**
     * 创建新的 Turn Item。
     *
     * @param turnId Turn 标识
     * @param type Item 类型
     * @param content 可读内容
     * @param toolName Tool 名称
     * @param callId Tool 调用标识
     * @param payloadJson 结构化负载
     * @return 尚未分配 sequence 的 Item
     */
    public static AgentTurnItem create(String turnId, TurnItemType type, String content,
                                       String toolName, String callId, String payloadJson) {
        AgentTurnItem item = new AgentTurnItem();
        item.setId(UUID.randomUUID().toString());
        item.setTurnId(turnId);
        item.setType(type);
        item.setContent(content);
        item.setToolName(toolName);
        item.setCallId(callId);
        item.setPayloadJson(payloadJson);
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }
}

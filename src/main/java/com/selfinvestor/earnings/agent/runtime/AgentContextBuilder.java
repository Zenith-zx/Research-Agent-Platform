package com.selfinvestor.earnings.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.tool.AgentTool;
import com.selfinvestor.earnings.agent.runtime.tool.ToolRegistry;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurnItem;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用 Thread 摘要、当前 Turn 历史和 Tool 清单构造每一步上下文。
 */
@Component
public class AgentContextBuilder {
    private static final int MAX_ITEM_TEXT_CHARS = 16000;

    @Resource
    private TurnItemService items;
    @Resource
    private ToolRegistry tools;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private EarningsProperties properties;
    @Resource
    private com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles profiles;

    /**
     * 构建当前决策需要的上下文，不加载其他 Turn 的完整历史。
     *
     * @param thread 长期研究 Thread
     * @param turn 当前 Turn
     * @return 模型输入上下文
     */
    public AgentContext build(AgentThread thread, AgentTurn turn) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("thread", thread(thread));
        root.put("turn", turn(turn));
        var profile = profiles.resolve(thread);
        root.put("profileName", profile.getProfileName());
        profiles.domain(thread).ifPresent(domain -> root.put("domain", domain.context(thread)));
        root.put("availableTools", tools.getAll().stream()
                .filter(tool -> profile.getAllowedTools().contains(tool.name())).map(this::tool).toList());
        var history = new java.util.ArrayList<>(items.history(turn.getId()).stream().map(this::item).toList());
        root.put("turnItems", history);
        String serialized = serialize(root);
        int maxChars = properties.getOpenai().getMaxInputChars();
        while (serialized.length() > maxChars) {
            int removable = -1;
            for (int index = 0; index < history.size() - 1; index++) {
                Object type = history.get(index).get("type");
                if (type != com.selfinvestor.earnings.agent.runtime.turn.TurnItemType.STEER_INPUT
                        && type != com.selfinvestor.earnings.agent.runtime.turn.TurnItemType.TRIGGER_INPUT
                        && type != com.selfinvestor.earnings.agent.runtime.turn.TurnItemType.USER_INPUT) {
                    removable = index;
                    break;
                }
            }
            if (removable < 0) {
                throw new IllegalStateException("CONTEXT_LIMIT: required input/state exceeds maxInputChars");
            }
            history.remove(removable);
            root.put("contextLimitation", "Older current-turn observations omitted for size; do not invent missing evidence");
            serialized = serialize(root);
        }
        AgentContext context = new AgentContext(serialized);
        context.setProfile(profile);
        return context;
    }

    /**
     * 创建 Thread 上下文字段。
     *
     * @param thread Thread 实体
     * @return Thread 字段
     */
    private Map<String, Object> thread(AgentThread thread) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", thread.getId());
        value.put("subjectType", thread.getSubjectType());
        value.put("subjectCode", thread.getSubjectCode());
        value.put("summary", thread.getSummary());
        value.put("memory", thread.getMemory());
        return value;
    }

    /**
     * 创建 Turn 目标和预算字段。
     *
     * @param turn Turn 实体
     * @return Turn 字段
     */
    private Map<String, Object> turn(AgentTurn turn) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", turn.getId());
        value.put("goal", turn.getGoal());
        value.put("triggerType", turn.getTriggerType());
        value.put("stepCount", turn.getStepCount());
        value.put("remainingSteps", turn.getMaxSteps() - turn.getStepCount());
        value.put("llmCallCount", turn.getLlmCallCount());
        value.put("remainingLlmCalls", turn.getMaxLlmCalls() - turn.getLlmCallCount());
        return value;
    }

    /**
     * 创建模型可读的 Tool 描述。
     *
     * @param tool Agent Tool
     * @return Tool 描述字段
     */
    private Map<String, Object> tool(AgentTool tool) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", tool.name());
        value.put("description", tool.description());
        value.put("schema", tool.schema());
        return value;
    }

    /**
     * 创建模型可读的历史 Item，并限制单条超长结果。
     *
     * @param item Turn Item
     * @return Item 字段
     */
    private Map<String, Object> item(AgentTurnItem item) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("sequence", item.getSequence());
        value.put("type", item.getType());
        value.put("content", truncate(item.getContent()));
        value.put("toolName", item.getToolName());
        value.put("callId", item.getCallId());
        value.put("payloadJson", truncate(item.getPayloadJson()));
        return value;
    }

    /**
     * 限制单个字段占用的上下文长度。
     *
     * @param value 原始文本
     * @return 裁剪文本
     */
    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ITEM_TEXT_CHARS) {
            return value;
        }
        return value.substring(0, MAX_ITEM_TEXT_CHARS) + "...[truncated]";
    }

    /**
     * 将上下文字段序列化为模型输入。
     *
     * @param value 上下文字段
     * @return JSON 文本
     */
    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize agent context", exception);
        }
    }
}

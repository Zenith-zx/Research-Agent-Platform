package com.selfinvestor.earnings.agent.runtime.tool;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 统一执行 Tool，并将未知 Tool 和业务异常转换为可观察结果。
 */
@Component
public class ToolExecutor {
    @Resource
    private ToolRegistry registry;
    @Resource
    private com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles profiles;

    /**
     * 执行指定 Tool，异常不会越过 Agent Loop。
     *
     * @param context Tool 上下文
     * @param toolName Tool 名称
     * @param arguments Tool 参数
     * @return 成功或失败结果
     */
    public ToolResult execute(AgentToolContext context, String toolName, Map<String, Object> arguments) {
        Instant started = Instant.now();
        if (!profiles.resolve(context.getThread()).getAllowedTools().contains(toolName)) {
            return ToolResult.failure("Tool not allowed by profile: " + toolName, elapsed(started));
        }
        AgentTool tool = registry.getTool(toolName);
        if (tool == null) {
            return ToolResult.failure("Unknown tool: " + toolName, elapsed(started));
        }
        try {
            Object data = tool.execute(context, arguments == null ? Map.of() : arguments);
            return ToolResult.success(data, elapsed(started));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            return ToolResult.failure(message, elapsed(started));
        }
    }

    /**
     * 计算从开始时间到现在的毫秒数。
     *
     * @param started 开始时间
     * @return 耗时毫秒数
     */
    private long elapsed(Instant started) {
        return Duration.between(started, Instant.now()).toMillis();
    }
}

package com.selfinvestor.earnings.agent.runtime.tool;

import java.util.Map;

/**
 * Agent 可以动态选择的确定性业务能力。
 */
public interface AgentTool {
    /**
     * 返回稳定且唯一的 Tool 名称。
     *
     * @return Tool 名称
     */
    String name();

    /**
     * 返回供模型选择 Tool 时使用的能力说明。
     *
     * @return Tool 说明
     */
    String description();

    /**
     * 返回 Tool 参数 Schema。
     *
     * @return 参数 Schema
     */
    ToolSchema schema();

    /**
     * 执行确定性业务能力。
     *
     * @param context Agent 运行上下文
     * @param arguments 模型提供的参数
     * @return Tool 数据
     */
    Object execute(AgentToolContext context, Map<String, Object> arguments);
}

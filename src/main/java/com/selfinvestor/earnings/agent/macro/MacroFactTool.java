package com.selfinvestor.earnings.agent.macro;

import com.selfinvestor.earnings.agent.runtime.tool.*;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * 将单一事实能力包装为共享 AgentTool，不具备 regime 写权限。
 */
@AllArgsConstructor
public class MacroFactTool implements AgentTool {
    private final String toolName;
    private final String explanation;
    private final ToolSchema parameters;
    private final MacroFacts facts;

    /**
     * 返回稳定的注册名称。
     */
    @Override
    public String name() {
        return toolName;
    }

    /**
     * 返回来源和能力限制。
     */
    @Override
    public String description() {
        return explanation;
    }

    /**
     * 返回模型可读取的工具参数。
     */
    @Override
    public ToolSchema schema() {
        return parameters;
    }

    /**
     * 只执行事实查询，异常交由共享 ToolExecutor 转换为 Observation。
     */
    @Override
    public Object execute(AgentToolContext context, Map<String, Object> arguments) {
        return facts.read(toolName, context, arguments);
    }
}

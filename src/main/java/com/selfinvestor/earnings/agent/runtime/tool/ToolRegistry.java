package com.selfinvestor.earnings.agent.runtime.tool;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册并查找所有可供 Agent 调用的 Tool。
 */
@Component
public class ToolRegistry {
    @Resource
    private List<AgentTool> tools;

    private final Map<String, AgentTool> registry = new LinkedHashMap<>();

    /**
     * 启动时建立稳定的 Tool 名称索引。
     */
    @PostConstruct
    void initialize() {
        for (AgentTool tool : tools) {
            AgentTool previous = registry.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("Duplicate agent tool: " + tool.name());
            }
        }
    }

    /**
     * 按名称查询 Tool。
     *
     * @param name Tool 名称
     * @return Tool；不存在时为空
     */
    public AgentTool getTool(String name) {
        return registry.get(name);
    }

    /**
     * 返回所有已注册 Tool。
     *
     * @return 只读 Tool 集合
     */
    public Collection<AgentTool> getAll() {
        return List.copyOf(registry.values());
    }
}

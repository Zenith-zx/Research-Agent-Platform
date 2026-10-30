package com.selfinvestor.earnings.agent.earnings.graph;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 固定 Earnings Graph 的节点名称。
 */
@Getter
@AllArgsConstructor
public enum AgentNodeName {
    PLANNER("创建首轮研究计划"),
    RESEARCH("按计划提取财报证据"),
    CRITIC("判断现有证据是否充分"),
    REPLANNER("根据证据缺口补充研究任务"),
    FINALIZER("生成最终结构化结论"),
    END("固定 Graph 执行结束");

    /**
     * 节点职责说明。
     */
    private final String desc;
}

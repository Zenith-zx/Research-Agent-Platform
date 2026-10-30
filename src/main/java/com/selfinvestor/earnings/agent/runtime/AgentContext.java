package com.selfinvestor.earnings.agent.runtime;

import lombok.Data;

/**
 * 一次模型决策需要的已压缩上下文。
 */
@Data
public class AgentContext {
    private String input;
    private com.selfinvestor.earnings.agent.runtime.profile.AgentProfile profile;

    /**
     * 创建基础上下文，兼容自定义或测试 DecisionModel。
     */
    public AgentContext(String input) {
        this.input = input;
    }
}

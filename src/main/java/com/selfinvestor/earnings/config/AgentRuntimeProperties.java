package com.selfinvestor.earnings.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态 Investment Agent Runtime 的开关和硬限制配置。
 */
@Data
@ConfigurationProperties(prefix = "self-investor.agent")
public class AgentRuntimeProperties {
    /**
     * 是否让财报监听走新的 Submission Runtime。
     */
    private boolean enabled;
    /**
     * 单个 Turn 允许的最大决策步数。
     */
    private int maxSteps = 12;
    /**
     * 单个 Turn 允许的最大模型调用次数。
     */
    private int maxLlmCalls = 12;
    /**
     * 相同 Tool 和参数允许出现的最大次数。
     */
    private int maxRepeatedToolCalls = 2;
    /**
     * 一个 Turn 允许的累计工具失败次数，跨 WAIT/恢复保留。
     */
    private int maxToolFailures = 5;
    /**
     * 进程内 Submission Queue 的容量。
     */
    private int queueCapacity = 100;
}

package com.selfinvestor.earnings.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 宏观 Profile 的独立开关和硬预算；依赖共享 Runtime 开关。
 */
@Data
@ConfigurationProperties(prefix = "self-investor.macro")
public class MacroAgentProperties {
    private boolean enabled;
    private int maxSteps = 15;
    private int maxLlmCalls = 15;
    private boolean notificationsEnabled;
}

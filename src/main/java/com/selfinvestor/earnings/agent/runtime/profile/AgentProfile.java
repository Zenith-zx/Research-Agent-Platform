package com.selfinvestor.earnings.agent.runtime.profile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Set;

/**
 * 领域行为配置；共享 Runtime 不依赖任何具体分析领域。
 */
@Data
@AllArgsConstructor
public class AgentProfile {
    private String profileName;
    private String systemPrompt;
    private Set<String> allowedTools;
    private int maxSteps;
    private int maxLlmCalls;
    private JsonNode decisionSchema;
}

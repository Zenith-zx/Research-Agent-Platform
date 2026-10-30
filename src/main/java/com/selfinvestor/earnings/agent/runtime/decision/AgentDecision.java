package com.selfinvestor.earnings.agent.runtime.decision;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 返回给 Agent Loop 的受约束结构化决策。
 */
@Data
@NoArgsConstructor
public class AgentDecision {
    private DecisionType type;
    private String reasoningSummary;
    private String toolName;
    private Map<String, Object> arguments = new LinkedHashMap<>();
    private String finalAnswer;
    private com.fasterxml.jackson.databind.JsonNode structuredResult;
}

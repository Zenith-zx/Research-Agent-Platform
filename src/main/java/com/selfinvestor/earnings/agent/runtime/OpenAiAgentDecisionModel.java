package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 通过现有 OpenAI Responses API 获取 AgentDecision。
 */
@Component
public class OpenAiAgentDecisionModel implements AgentDecisionModel {
    @Resource
    private OpenAiClient client;
    @Resource
    private AgentDecisionPromptService prompts;
    @Resource
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    /**
     * 请求严格 JSON Schema 约束的下一步决策。
     *
     * @param context 当前 Agent 上下文
     * @return 结构化决策
     */
    @Override
    public AgentDecision decide(AgentContext context) {
        var profile = context.getProfile();
        WireDecision wire = client.generateStructured(context.getInput(),
                profile == null ? prompts.systemPrompt() : profile.getSystemPrompt(),
                profile == null ? prompts.schema() : profile.getDecisionSchema(),
                "investment_agent_decision", WireDecision.class);
        AgentDecision decision = new AgentDecision();
        decision.setType(wire.getType());
        decision.setReasoningSummary(wire.getReasoningSummary());
        decision.setToolName(wire.getToolName());
        decision.setFinalAnswer(wire.getFinalAnswer());
        decision.setStructuredResult(wire.getStructuredResult());
        try {
            decision.setArguments(mapper.readValue(wire.getArgumentsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() { }));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("argumentsJson must encode a JSON object", exception);
        }
        return decision;
    }

    /**
     * 严格 Schema 的传输格式；动态工具参数以 JSON 字符串承载，避免开放对象 Schema。
     */
    @lombok.Data
    public static class WireDecision {
        private com.selfinvestor.earnings.agent.runtime.decision.DecisionType type;
        private String reasoningSummary;
        private String toolName;
        private String argumentsJson;
        private String finalAnswer;
        private com.fasterxml.jackson.databind.JsonNode structuredResult;
    }
}

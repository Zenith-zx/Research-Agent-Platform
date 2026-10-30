package com.selfinvestor.earnings.agent.macro;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.macro.model.*;
import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.agent.runtime.profile.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import com.selfinvestor.earnings.config.MacroAgentProperties;
import com.selfinvestor.earnings.repository.AgentThreadRepository;
import com.selfinvestor.earnings.repository.MacroRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 宏观领域适配器：复用共享 Runtime，只补充 Profile、状态上下文与最终提交。
 */
@Component
public class MacroAgentDomain implements AgentDomain {
    @Resource
    private MacroAgentProperties properties;
    @Resource
    private MacroStateService states;
    @Resource
    private MacroRepository repository;
    @Resource
    private AgentThreadRepository threads;
    @Resource
    private ObjectMapper mapper;
    private AgentProfile profile;

    /**
     * 加载宏观系统提示和严格输出 Schema，默认预算为十五步/十五次模型调用。
     */
    @PostConstruct
    void initialize() {
        if (properties.getMaxSteps() < 1 || properties.getMaxLlmCalls() < 1) {
            throw new IllegalArgumentException("Macro budgets must be positive");
        }
        try {
            profile = new AgentProfile("US_MACRO_ANALYST", read("prompts/macro-agent-system.txt"),
                    Set.of("get_macro_release", "get_macro_history", "get_cpi_components", "get_fomc_statement", "compare_previous_fomc_statement", "get_treasury_yields", "get_dollar_index", "get_vix", "get_market_index", "get_oil_price", "get_macro_news"),
                    properties.getMaxSteps(), properties.getMaxLlmCalls(),
                    mapper.readTree(read("prompts/macro-decision-schema.json")));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize macro profile", exception);
        }
    }

    /**
     * V1 只接管 US_MACRO，其他未来主题不能误用此领域处理器。
     */
    @Override
    public boolean supports(AgentThread thread) {
        return thread.getSubjectType() == SubjectType.MACRO && "US_MACRO".equals(thread.getSubjectCode());
    }

    /**
     * 返回宏观分析的行为和工具权限配置。
     */
    @Override
    public AgentProfile profile() {
        return profile;
    }

    /**
     * 加载结构化状态和最近五次摘要/变更，不加载历史工具结果。
     */
    @Override
    public Map<String, Object> context(AgentThread thread) {
        return Map.of("macroState", states.getOrCreate(thread.getId()),
                "recentAnalyses", repository.recentAnalyses(thread.getId()).stream().map(record ->
                        Map.of("turnId", record.getTurnId(), "summary", limited(record.getSummary(), 1000),
                                "at", record.getCreatedAt())).toList(),
                "recentStateChanges", repository.recentChanges(thread.getId()).stream().map(this::compactRecord).toList());
    }

    /**
     * 在共享完成事务内严格验证、稀疏更新、保存审计和高价值记忆。
     */
    @Override
    public String complete(AgentThread thread, AgentTurn turn, AgentDecision decision) {
        MacroAnalysisResult result = parse(decision);
        MacroState updated = states.apply(thread.getId(), result.getProposedStateChange());
        result.setProposedStateChange(effectiveChanges(result.getProposedStateChange()));
        MacroRecord record = new MacroRecord();
        record.setTurnId(turn.getId());
        record.setThreadId(thread.getId());
        record.setResultJson(json(result));
        record.setChangesJson(json(result.getProposedStateChange()));
        record.setSummary(result.getSummary());
        record.setStateVersion(updated.getVersion());
        record.setCreatedAt(LocalDateTime.now());
        repository.insertAnalysis(record);
        AgentThread fresh = threads.findById(thread.getId()).orElseThrow();
        fresh.setSummary(limited(result.getSummary(), 1200));
        fresh.setMemory(json(Map.of("currentThesis", limited(result.getSummary(), 1200), "regimes", updated,
                "recentChanges", repository.recentChanges(thread.getId()).stream().limit(3)
                        .map(this::compactRecord).toList())));
        fresh.setUpdatedAt(LocalDateTime.now());
        threads.update(fresh);
        return render(turn, result, updated);
    }

    /**
     * 验证后去除 from 等于 to 的提案，审计和记忆只记载真正生效的维度。
     */
    private MacroStateChange effectiveChanges(MacroStateChange proposed) {
        com.fasterxml.jackson.databind.node.ObjectNode changes = mapper.valueToTree(proposed);
        List<String> unchanged = new ArrayList<>();
        changes.fields().forEachRemaining(entry -> {
            if (entry.getValue().path("from").equals(entry.getValue().path("to"))) {
                unchanged.add(entry.getKey());
            }
        });
        changes.remove(unchanged);
        return mapper.convertValue(changes, MacroStateChange.class);
    }

    /**
     * 最近记录只保留简短结论和变化，不重复装入完整报告或工具结果。
     */
    private Map<String, Object> compactRecord(MacroRecord record) {
        Map<String, String> changes = new LinkedHashMap<>();
        try {
            mapper.readTree(record.getChangesJson()).fields().forEachRemaining(entry -> {
                var change = entry.getValue();
                changes.put(entry.getKey(), change.path("from").asText() + " → " + change.path("to").asText()
                        + ": " + limited(change.path("reason").asText(), 180));
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid stored macro changes", exception);
        }
        return Map.of("turnId", record.getTurnId(), "summary", limited(record.getSummary(), 1000),
                "changes", changes, "at", record.getCreatedAt());
    }

    /**
     * 压缩记忆中的文本并标注省略，完整结果始终保留在 MACRO_ANALYSIS。
     */
    private String limited(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length) + "…[完整内容见分析记录]";
    }

    /**
     * 严格拒绝未知字段、未知枚举和缺失必需结果，避免静默丢弃模型变更。
     */
    private MacroAnalysisResult parse(AgentDecision decision) {
        if (decision.getStructuredResult() == null || decision.getStructuredResult().isNull()) {
            throw new IllegalArgumentException("Macro FINAL_ANSWER requires structuredResult");
        }
        try {
            MacroAnalysisResult result = mapper.readerFor(MacroAnalysisResult.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(decision.getStructuredResult());
            if (result.getSummary() == null || result.getSummary().isBlank() || result.getSummary().length() > 6000
                    || result.getWhatChanged() == null || result.getMarketReaction() == null
                    || result.getProposedStateChange() == null || result.getConfidence() == null
                    || result.getKeyFindings() == null || result.getMarketImpacts() == null
                    || result.getResearchImplications() == null || result.getRisks() == null) {
                throw new IllegalArgumentException("Incomplete macro final result");
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid macro structuredResult", exception);
        }
    }

    /**
     * 根据已验证并持久化的结果生成固定阅读结构，避免文本和最终状态不一致。
     */
    private String render(AgentTurn turn, MacroAnalysisResult result, MacroState state) {
        return "# Macro Event Summary\n\n" + result.getSummary()
                + "\n\n## Event\n\n" + turn.getTriggerType()
                + "\n\n## What Changed\n\n" + result.getWhatChanged()
                + "\n\n## Key Interpretation\n\n" + bullets(result.getKeyFindings())
                + "\n\n## Macro Regime Impact\n\n" + stateLines(state)
                + "\n\n## Market Transmission\n\n" + bullets(result.getMarketImpacts())
                + "\n\n## Market Reaction\n\n" + result.getMarketReaction()
                + "\n\n## MacroState Changes\n\n" + changeLines(result.getProposedStateChange())
                + "\n\n## Investment Implications (Research Only)\n\n" + bullets(result.getResearchImplications())
                + "\n\n## Risks / Alternative Interpretation\n\n" + bullets(result.getRisks())
                + "\n\n## Confidence\n\n" + result.getConfidence();
    }

    /**
     * 给读者展示枚举及中文解释，不要求读者自行理解状态代码。
     */
    private String stateLines(MacroState state) {
        return bullets(List.of(
                "Growth: " + state.getGrowth() + " — " + state.getGrowth().getDesc(),
                "Inflation: " + state.getInflation() + " — " + state.getInflation().getDesc(),
                "Labor: " + state.getLabor() + " — " + state.getLabor().getDesc(),
                "Fed: " + state.getFed() + " — " + state.getFed().getDesc(),
                "Liquidity: " + state.getLiquidity() + " — " + state.getLiquidity().getDesc(),
                "Rates: " + state.getRates() + " — " + state.getRates().getDesc(),
                "Dollar: " + state.getDollar() + " — " + state.getDollar().getDesc(),
                "Risk: " + state.getRisk() + " — " + state.getRisk().getDesc()));
    }

    /**
     * 将实际生效的稀疏变更展示为前态到后态和理由，无变化时明确说明。
     */
    private String changeLines(MacroStateChange change) {
        com.fasterxml.jackson.databind.JsonNode node = mapper.valueToTree(change);
        List<String> lines = new ArrayList<>();
        node.fields().forEachRemaining(entry -> lines.add(entry.getKey() + ": "
                + entry.getValue().path("from").asText() + " → " + entry.getValue().path("to").asText()
                + " — " + entry.getValue().path("reason").asText()));
        return lines.isEmpty() ? "无实质宏观 regime 变化，状态版本和更新时间保持不变。" : bullets(lines);
    }

    /**
     * 将模型列表转换为 Markdown，null 元素属于无效输出。
     */
    private String bullets(List<String> values) {
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Macro result lists cannot contain null");
        }
        return String.join("\n", values.stream().map(value -> "- " + value).toList());
    }

    /**
     * 序列化状态、结果和精简记忆。
     */
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot serialize macro analysis", exception);
        }
    }

    /**
     * 完整读取 UTF-8 Prompt 资源，并关闭流。
     */
    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

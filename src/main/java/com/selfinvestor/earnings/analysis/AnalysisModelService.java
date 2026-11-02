package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.llm.LlmClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 财报旧链与市场压力的业务提示适配，供应商选择交给共享 LlmClient。
 */
@Service
public class AnalysisModelService {
    @Resource
    private LlmClient client;
    @Resource
    private ObjectMapper mapper;
    private String financialPrompt;
    private JsonNode financialSchema;
    private String marketPrompt;
    private JsonNode marketSchema;

    /**
     * 加载现有业务 Prompt 与 Schema，不改变分析输出契约。
     */
    @PostConstruct
    void initialize() {
        financialPrompt = read("prompts/financial-analysis-system.txt");
        marketPrompt = read("prompts/market-stress-system-prompt.txt");
        try {
            financialSchema = mapper.readTree(read("prompts/financial-analysis-schema.json"));
            marketSchema = mapper.readTree(read("prompts/market-stress-analysis-schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid analysis JSON Schema", exception);
        }
    }

    /**
     * 通过当前策略分析财报，保留旧版本禁止捏造分析师预期的业务约束。
     */
    public FinancialAnalysisDTO analyze(String input) {
        FinancialAnalysisDTO analysis = client.generateStructured(input, financialPrompt, financialSchema,
                "financial_analysis", FinancialAnalysisDTO.class);
        if (analysis.isConsensusComparisonAvailable()) {
            throw new IllegalArgumentException("Consensus comparison is unavailable in this MVP");
        }
        return analysis;
    }

    /**
     * 通过同一策略执行市场压力分析。
     */
    public MarketStressAnalysisDTO analyzeMarketStress(String input) {
        return client.generateStructured(input, marketPrompt, marketSchema,
                "market_stress_analysis", MarketStressAnalysisDTO.class);
    }

    /**
     * 完整读取 UTF-8 资源并关闭流。
     */
    private String read(String path) {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read analysis resource: " + path, exception);
        }
    }
}

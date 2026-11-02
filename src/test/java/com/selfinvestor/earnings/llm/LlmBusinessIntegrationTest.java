package com.selfinvestor.earnings.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.selfinvestor.earnings.agent.runtime.*;
import com.selfinvestor.earnings.agent.runtime.decision.DecisionType;
import com.selfinvestor.earnings.agent.runtime.profile.AgentProfile;
import com.selfinvestor.earnings.analysis.AnalysisModelService;
import com.selfinvestor.earnings.config.DeepSeekProperties;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.config.LlmProperties;
import com.selfinvestor.earnings.llm.strategy.DeepSeekLlmStrategy;
import com.selfinvestor.earnings.llm.strategy.OpenAiLlmStrategy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class LlmBusinessIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private MockWebServer server;
    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        runner = new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
                .withPropertyValues("self-investor.llm.provider=DEEPSEEK",
                        "self-investor.deepseek.base-url=" + server.url("/"),
                        "self-investor.deepseek.api-key=test-only-key",
                        "self-investor.deepseek.model=test-deepseek",
                        "self-investor.deepseek.max-input-chars=12345",
                        "self-investor.deepseek.read-timeout=5s");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void springBindsProviderAndLoadsAllExistingBusinessSchemas() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(LlmClient.class);
            assertThat(context.getBean(LlmClient.class).modelName()).isEqualTo("test-deepseek");
            assertThat(context.getBean(LlmClient.class).maxInputChars()).isEqualTo(12345);
            assertThat(context.getBean(DeepSeekProperties.class).getReadTimeout()).hasSeconds(5);
            assertThat(context.getBean(DeepSeekProperties.class).toString()).doesNotContain("test-only-key");
            var parser = context.getBean(StructuredOutputParser.class);
            var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:prompts/**/*schema.json");
            assertThat(resources.length).isGreaterThanOrEqualTo(9);
            for (var resource : resources) {
                try (var stream = resource.getInputStream()) {
                    assertThat(parser.prepare(mapper.readTree(stream))).isNotNull();
                }
            }
        });
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void unknownProviderFailsConfigurationBinding() {
        runner.withPropertyValues("self-investor.llm.provider=TYPO").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void legacyFinancialFacadeUsesDeepSeekAndEnforcesRefsRangesAndConsensusGuard() throws Exception {
        ObjectNode financial = (ObjectNode) mapper.readTree("""
                {"company":"Credo","ticker":"CRDO","fiscalQuarter":null,"headline":"Test",
                 "financials":{"revenue":{"value":"$100m","yoyGrowth":12,"qoqGrowth":null},
                   "grossMargin":null,"operatingMargin":null,"operatingIncome":null,"netIncome":null,
                   "eps":null,"freeCashFlow":null,"capex":null,"cash":null,"debt":null},
                 "guidance":{"nextQuarterRevenueLow":null,"nextQuarterRevenueHigh":null,
                   "grossMargin":null,"fullYearGuidance":null},
                 "segments":[],"positives":[],"negatives":[],"risks":[],"managementCommentary":[],
                 "fundamentalTrend":"STABLE","fundamentalTrendReason":null,"investmentThesisChanged":false,
                 "investmentThesisChangeReason":null,"consensusComparisonAvailable":false,
                 "consensusComparisonNote":"No consensus data","qualityScore":5,"summary":"Test fixture"}
                """);
        enqueue(financial);
        var missingNested = financial.deepCopy();
        ((ObjectNode) missingNested.at("/financials/revenue")).remove("yoyGrowth");
        enqueue(missingNested);
        enqueue(financial.deepCopy().put("qualityScore", 11));
        enqueue(financial.deepCopy().put("consensusComparisonAvailable", true));
        runner.run(context -> {
            var service = context.getBean(AnalysisModelService.class);
            assertThat(service.analyze("test filing").getTicker()).isEqualTo("CRDO");
            for (int i = 0; i < 3; i++) {
                assertThatThrownBy(() -> service.analyze("test filing")).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("JSON Schema");
            }
        });
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void sharedMacroDecisionAdapterPreservesToolArgumentsAndRejectsInvalidNestedResult() throws Exception {
        ObjectNode decision = (ObjectNode) mapper.readTree("""
                {"type":"TOOL_CALL","reasoningSummary":"Need historical data","toolName":"get_macro_history",
                 "argumentsJson":"{}","finalAnswer":null,"structuredResult":null}
                """);
        decision.put("argumentsJson", "{\"indicator\":\"CPI\"}");
        enqueue(decision);
        ObjectNode result = (ObjectNode) mapper.readTree("""
                {"summary":"Test fixture","whatChanged":"No change","keyFindings":[],"marketImpacts":[],
                 "marketReaction":"No data","researchImplications":[],"risks":[],"confidence":"LOW",
                 "proposedStateChange":{"growth":null,"inflation":null,"labor":null,"fed":null,
                  "liquidity":null,"rates":null,"dollar":null,"risk":null}}
                """);
        decision.put("type", "FINAL_ANSWER").putNull("toolName").put("argumentsJson", "{}").put("finalAnswer", "Test");
        decision.set("structuredResult", result);
        enqueue(decision);
        ((ObjectNode) result.path("proposedStateChange")).putObject("growth")
                .put("from", "UNKNOWN").put("to", "NOT_A_VALID_STATE").put("reason", "Test");
        enqueue(decision);
        runner.run(context -> {
            var model = context.getBean(DefaultAgentDecisionModel.class);
            var input = new AgentContext("Test macro release");
            try (var stream = new ClassPathResource("prompts/macro-decision-schema.json").getInputStream()) {
                input.setProfile(new AgentProfile("US_MACRO_ANALYST", "macro instructions",
                        Set.of("get_macro_history"), 15, 15, mapper.readTree(stream)));
            }
            var toolCall = model.decide(input);
            assertThat(toolCall.getType()).isEqualTo(DecisionType.TOOL_CALL);
            assertThat(toolCall.getArguments()).containsEntry("indicator", "CPI");
            assertThat(model.decide(input).getStructuredResult().path("confidence").asText()).isEqualTo("LOW");
            assertThatThrownBy(() -> model.decide(input)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JSON Schema");
        });
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void marketStressFacadeUsesTheSameDeepSeekStrategy() throws Exception {
        enqueue(mapper.readTree("""
                {"symbol":"QQQ","analysisTimestamp":"2026-11-02T00:00:00Z","marketRegime":"UNKNOWN",
                 "primaryDrivers":[],"eventStage":"UNKNOWN","reversibility":"UNKNOWN",
                 "fundamentalDamage":"UNKNOWN","financialStress":"UNKNOWN","eventPricing":"UNKNOWN",
                 "secondOrderEffectsPricing":"UNKNOWN","negativeNewsSensitivity":"UNKNOWN",
                 "sellerExhaustion":"UNKNOWN","technicalCondition":"UNKNOWN","technicalScore":0,
                 "meanReversionScore":0,"action":"WAIT_FOR_CONFIRMATION","marketIsTrading":null,
                 "reasoningSummary":"Test fixture","confirmationSignals":[],"invalidationSignals":[],
                 "keyRisks":[],"confidence":null}
                """));
        runner.run(context -> assertThat(context.getBean(AnalysisModelService.class)
                .analyzeMarketStress("test snapshot").getSymbol()).isEqualTo("QQQ"));
        assertThat(server.getRequestCount()).isOne();
    }

    private void enqueue(com.fasterxml.jackson.databind.JsonNode content) {
        var response = mapper.createObjectNode();
        response.putArray("choices").addObject().put("finish_reason", "stop")
                .putObject("message").put("content", content.toString());
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response.toString()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({LlmProperties.class, DeepSeekProperties.class, EarningsProperties.class})
    @Import({LlmClient.class, StructuredOutputParser.class, LlmHttpTransport.class,
            DeepSeekLlmStrategy.class, OpenAiLlmStrategy.class, AnalysisModelService.class,
            DefaultAgentDecisionModel.class, AgentDecisionPromptService.class})
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}

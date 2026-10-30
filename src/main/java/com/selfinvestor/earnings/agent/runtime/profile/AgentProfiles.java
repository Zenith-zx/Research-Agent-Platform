package com.selfinvestor.earnings.agent.runtime.profile;

import com.selfinvestor.earnings.agent.runtime.AgentDecisionPromptService;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 按领域选择 Profile，默认财报能力不暴露宏观或未来其他领域工具。
 */
@Component
public class AgentProfiles {
    @Resource
    private List<AgentDomain> domains;
    @Resource
    private AgentDecisionPromptService prompts;
    @Resource
    private AgentRuntimeProperties properties;

    /**
     * 查询 Thread 的领域适配器。
     */
    public Optional<AgentDomain> domain(AgentThread thread) {
        return domains.stream().filter(domain -> domain.supports(thread)).findFirst();
    }

    /**
     * 返回领域 Profile 或兼容财报 Profile。
     */
    public AgentProfile resolve(AgentThread thread) {
        if (thread.getSubjectType() == com.selfinvestor.earnings.agent.runtime.thread.SubjectType.MACRO
                && domain(thread).isEmpty()) {
            throw new IllegalArgumentException("No profile registered for macro subject: " + thread.getSubjectCode());
        }
        return domain(thread).map(AgentDomain::profile).orElseGet(() ->
                new AgentProfile("COMPANY_ANALYST", prompts.systemPrompt(),
                        Set.of("get_company_information", "get_earnings_report", "get_historical_filings"),
                        properties.getMaxSteps(), properties.getMaxLlmCalls(), prompts.schema()));
    }
}

package com.selfinvestor.earnings.agent.earnings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.agent.earnings.model.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsModelEnumDescriptionTest {

    private static final Class<?>[] DESCRIBED_ENUMS = {
            AgentStatus.class,
            CriticDecision.class,
            EarningsQuality.class,
            EvidenceSourceType.class,
            ExpectationGap.class,
            FundamentalTrend.class,
            GrowthTrend.class,
            GuidanceAssessment.class,
            HypothesisStatus.class,
            MarginTrend.class,
            ResearchCategory.class,
            TaskPriority.class,
            TaskStatus.class
    };

    @Test
    void everyEarningsModelEnumValueHasDescription() throws Exception {
        for (Class<?> enumType : DESCRIBED_ENUMS) {
            Method getDesc = enumType.getMethod("getDesc");
            for (Object value : enumType.getEnumConstants()) {
                assertThat((String) getDesc.invoke(value))
                        .as("%s.%s desc", enumType.getSimpleName(), value)
                        .isNotBlank();
            }
        }
    }

    @Test
    void descriptionDoesNotChangeStructuredOutputEnumValue() throws Exception {
        assertThat(new ObjectMapper().writeValueAsString(CriticDecision.NEED_MORE_RESEARCH))
                .isEqualTo("\"NEED_MORE_RESEARCH\"");
    }
}

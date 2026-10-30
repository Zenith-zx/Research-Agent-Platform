package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.agent.runtime.submission.SubmissionDispatcher;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "self-investor.agent.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentRuntimeEnabledContextTest {
    @Resource
    private SubmissionDispatcher dispatcher;

    @Test
    void enabledRuntimeCreatesDispatcher() {
        assertThat(dispatcher).isNotNull();
    }
}

package com.selfinvestor.earnings.agent.runtime;

import com.selfinvestor.earnings.agent.runtime.tool.AgentTool;
import com.selfinvestor.earnings.agent.runtime.tool.AgentToolContext;
import com.selfinvestor.earnings.agent.runtime.tool.ToolExecutor;
import com.selfinvestor.earnings.agent.runtime.tool.ToolRegistry;
import com.selfinvestor.earnings.agent.runtime.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolExecutorTest {
    private ToolRegistry registry;
    private ToolExecutor executor;

    @BeforeEach
    void setUp() {
        registry = mock(ToolRegistry.class);
        executor = new ToolExecutor();
        ReflectionTestUtils.setField(executor, "registry", registry);
        var profiles = mock(com.selfinvestor.earnings.agent.runtime.profile.AgentProfiles.class);
        when(profiles.resolve(any())).thenReturn(
                new com.selfinvestor.earnings.agent.runtime.profile.AgentProfile("TEST", "",
                        java.util.Set.of("missing_tool", "broken_tool"), 12, 12, null));
        ReflectionTestUtils.setField(executor, "profiles", profiles);
    }

    @Test
    void unknownToolReturnsFailureObservation() {
        ToolResult result = executor.execute(mock(AgentToolContext.class), "missing_tool", Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Unknown tool");
    }

    @Test
    void toolExceptionReturnsFailureObservation() {
        AgentTool tool = mock(AgentTool.class);
        when(registry.getTool("broken_tool")).thenReturn(tool);
        when(tool.execute(any(), anyMap())).thenThrow(new IllegalStateException("provider unavailable"));

        ToolResult result = executor.execute(mock(AgentToolContext.class), "broken_tool", Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("provider unavailable");
    }
}

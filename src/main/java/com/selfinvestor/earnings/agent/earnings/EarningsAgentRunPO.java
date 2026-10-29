package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.model.AgentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class EarningsAgentRunPO {
    private String runId;
    private Long filingId;
    private String ticker;
    private String fiscalQuarter;
    private AgentStatus status;
    private int currentRound;
    private int stepCount;
    private int llmCallCount;
    private double confidence;
    private Instant startTime;
    private Instant endTime;
    private String stateJson;
    private String finalResult;
    private String errorMessage;
}

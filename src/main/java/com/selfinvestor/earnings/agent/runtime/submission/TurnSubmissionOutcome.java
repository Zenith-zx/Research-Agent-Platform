package com.selfinvestor.earnings.agent.runtime.submission;

import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * start-or-steer 的结果和目标 Turn。
 */
@Data
@AllArgsConstructor
public class TurnSubmissionOutcome {
    private TurnSubmissionResult result;
    private AgentTurn turn;
}

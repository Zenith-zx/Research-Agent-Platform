package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 动态 Agent Turn 成功完成事件。
 */
@Data
@AllArgsConstructor
public class AgentTurnCompletedEvent {
    private AgentThread thread;
    private AgentTurn turn;
    private Submission submission;
}

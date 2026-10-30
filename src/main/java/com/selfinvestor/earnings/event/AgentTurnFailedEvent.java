package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 动态 Agent Turn 失败或达到限制事件。
 */
@Data
@AllArgsConstructor
public class AgentTurnFailedEvent {
    private AgentThread thread;
    private AgentTurn turn;
    private Submission submission;
}

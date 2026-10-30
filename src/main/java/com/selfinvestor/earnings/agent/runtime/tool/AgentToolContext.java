package com.selfinvestor.earnings.agent.runtime.tool;

import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Tool 执行时可读取但不可直接修改的运行上下文。
 */
@Data
@AllArgsConstructor
public class AgentToolContext {
    private AgentThread thread;
    private AgentTurn turn;
    private Submission submission;
}

package com.selfinvestor.earnings.agent.runtime.profile;

import com.selfinvestor.earnings.agent.runtime.decision.AgentDecision;
import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import java.util.Map;

/**
 * 可扩展领域适配点；只提供 Profile、上下文和最终结果处理，不实现另一套循环。
 */
public interface AgentDomain {
    /**
     * 是否负责此 Thread。
     */
    boolean supports(AgentThread thread);

    /**
     * 返回模型行为和能力边界。
     */
    AgentProfile profile();

    /**
     * 返回有界的领域状态上下文。
     */
    Map<String, Object> context(AgentThread thread);

    /**
     * 在共享完成事务内校验并保存领域结果，返回人类可读文本。
     */
    String complete(AgentThread thread, AgentTurn turn, AgentDecision decision);
}

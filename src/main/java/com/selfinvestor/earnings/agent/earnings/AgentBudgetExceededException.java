package com.selfinvestor.earnings.agent.earnings;

/**
 * Agent 执行超过 Java 侧硬预算时抛出的异常。
 */
public class AgentBudgetExceededException extends RuntimeException {
    /**
     * 创建预算超限异常。
     *
     * @param message 超限原因
     */
    public AgentBudgetExceededException(String message) {
        super(message);
    }
}

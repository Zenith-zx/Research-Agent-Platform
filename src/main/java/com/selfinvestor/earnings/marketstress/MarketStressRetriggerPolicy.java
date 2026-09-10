package com.selfinvestor.earnings.marketstress;

/**
 * 冷却期内重新触发市场分析的扩展策略。
 */
public interface MarketStressRetriggerPolicy {
    /**
     * 判断是否存在足以绕过冷却期的新变化。
     *
     * @param symbol 标的
     * @return 是否允许重新触发
     */
    boolean shouldRetrigger(String symbol);
}

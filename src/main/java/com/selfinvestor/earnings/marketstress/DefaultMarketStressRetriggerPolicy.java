package com.selfinvestor.earnings.marketstress;

import org.springframework.stereotype.Component;

/**
 * 第一版默认不在冷却期内重新触发。
 */
@Component
public class DefaultMarketStressRetriggerPolicy implements MarketStressRetriggerPolicy {
    /**
     * 第一版不实现重大新闻绕过逻辑。
     *
     * @param symbol 标的
     * @return 固定返回否
     */
    @Override
    public boolean shouldRetrigger(String symbol) {
        return false;
    }
}

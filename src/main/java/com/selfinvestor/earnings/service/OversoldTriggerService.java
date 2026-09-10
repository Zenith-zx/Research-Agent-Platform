package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.config.MarketStressProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 超卖触发判断服务。
 */
@Service
public class OversoldTriggerService {
    @Resource
    private MarketStressProperties properties;

    /**
     * 判断 RSI 是否低于配置阈值。
     *
     * @param rsi RSI 结果
     * @return 是否触发市场研究
     */
    public boolean shouldTrigger(Optional<BigDecimal> rsi) {
        if (rsi == null || rsi.isEmpty()) {
            return false;
        }
        return rsi.get().compareTo(properties.getRsiTrigger()) < 0;
    }
}

package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.service.MarketStressService;
import com.selfinvestor.earnings.service.UsMarketSessionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 市场压力定时扫描器。
 */
@Component
@Slf4j
@ConditionalOnExpression("${market-stress.enabled:false} and ${market-stress.scheduler.enabled:true}")
public class MarketStressScheduler {
    @Resource
    private MarketStressProperties properties;
    @Resource
    private MarketStressService marketStressService;
    @Resource
    private UsMarketSessionService marketSessionService;

    /**
     * 按配置间隔扫描市场压力标的。
     */
    @Scheduled(initialDelayString = "${market-stress.scheduler.initial-delay-ms:15000}",
            fixedDelayString = "#{${market-stress.scheduler.interval-minutes:30} * 60000}")
    public void scan() {
        if (properties.getScheduler().isMarketHoursOnly() && !marketSessionService.isRegularSessionNow()) {
            return;
        }
        if (CollectionUtils.isEmpty(properties.getSymbols())) {
            return;
        }
        for (String symbol : properties.getSymbols()) {
            try {
                marketStressService.scan(symbol);
            } catch (RuntimeException exception) {
                log.error("Market scan failed: symbol={} error={}", symbol, exception.getMessage(), exception);
            }
        }
    }
}

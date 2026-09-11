package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.marketstress.provider.IbkrWebApiClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * IBKR Client Portal Brokerage Session 保活调度器。
 */
@Component
@Slf4j
@ConditionalOnExpression("${market-stress.enabled:false} and "
        + "'${market-stress.provider.market-data:alpha-vantage}' == 'ibkr'")
public class IbkrSessionKeepAliveScheduler {
    @Resource
    private IbkrWebApiClient client;

    /**
     * 每四分钟调用一次非行情 tickle 接口，避免 Brokerage Session 因空闲失效。
     */
    @Scheduled(initialDelayString = "${market-stress.provider.ibkr.keep-alive-initial-delay-ms:30000}",
            fixedDelayString = "${market-stress.provider.ibkr.keep-alive-interval-ms:240000}")
    public void keepAlive() {
        try {
            client.keepAlive();
            log.debug("IBKR brokerage session keep-alive completed");
        } catch (RuntimeException exception) {
            log.warn("IBKR brokerage session keep-alive failed: reason={}", exception.getMessage());
        }
    }
}

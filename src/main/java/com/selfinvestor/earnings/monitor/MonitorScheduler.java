package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.service.CompanyService;
import com.selfinvestor.earnings.company.CompanyPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "earnings.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorScheduler {
    @Resource
    private CompanyService companies;
    @Resource
    private FilingMonitor monitor;

    /**
     * 定时轮询所有启用监听的公司。
     */
    @Scheduled(initialDelayString = "${earnings.monitor.initial-delay-ms:10000}",
            fixedDelayString = "#{${earnings.monitor.interval-seconds:30} * 1000}")
    public void poll() {
        for (CompanyPO company : companies.enabledCompanies()) {
            try {
                monitor.poll(company);
            } catch (RuntimeException exception) {
                log.error("SEC monitor failed for {}: {}", company.getTicker(), exception.getMessage());
            }
        }
    }
}

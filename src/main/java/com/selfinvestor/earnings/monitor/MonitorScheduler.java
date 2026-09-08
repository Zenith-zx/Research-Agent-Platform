package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "earnings.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorScheduler {
    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);
    private final CompanyService companies;
    private final FilingMonitor monitor;

    public MonitorScheduler(CompanyService companies, FilingMonitor monitor) {
        this.companies = companies;
        this.monitor = monitor;
    }

    @Scheduled(initialDelayString = "${earnings.monitor.initial-delay-ms:10000}",
            fixedDelayString = "#{${earnings.monitor.interval-seconds:30} * 1000}")
    public void poll() {
        for (var company : companies.enabledCompanies()) {
            try { monitor.poll(company); }
            catch (RuntimeException exception) { log.error("SEC monitor failed for {}: {}", company.getTicker(), exception.getMessage()); }
        }
    }
}

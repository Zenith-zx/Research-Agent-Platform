package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.service.FilingService;
import com.selfinvestor.earnings.sec.SecClient;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

@Component
public class SecFilingMonitor implements FilingMonitor {
    private static final Set<String> PERIODIC_FORMS = new HashSet<>(Arrays.asList("10-Q", "10-K"));
    @Resource
    private SecClient secClient;
    @Resource
    private FilingService filingService;
    @Resource
    private EarningsProperties properties;

    /**
     * 拉取并注册公司的财报相关 Filing。
     *
     * @param company 关注公司
     */
    @Override
    public void poll(CompanyPO company) {
        LocalDate cutoffDate = LocalDate.now(ZoneOffset.UTC)
                .minusDays(properties.getMonitor().getFilingLookbackDays());
        secClient.recentFilings(company).stream()
                .filter(this::isEarningsRelated)
                .filter(metadata -> !metadata.getFilingDate().isBefore(cutoffDate))
                .forEach(metadata -> filingService.registerIfNew(company, metadata));
    }

    /**
     * 判断 SEC Filing 是否与财报相关。
     *
     * @param filing SEC Filing 元数据
     * @return 是否需要进入处理链路
     */
    boolean isEarningsRelated(SecFilingMetadataDTO filing) {
        String form = filing.getFormType().toUpperCase(Locale.ROOT);
        if (PERIODIC_FORMS.contains(form)) {
            return true;
        }
        return form.equals("8-K") && filing.getItems() != null && filing.getItems().contains("2.02");
    }
}

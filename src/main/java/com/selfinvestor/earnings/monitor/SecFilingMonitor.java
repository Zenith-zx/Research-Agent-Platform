package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.filing.FilingService;
import com.selfinvestor.earnings.sec.SecClient;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class SecFilingMonitor implements FilingMonitor {
    private static final Set<String> PERIODIC_FORMS = Set.of("10-Q", "10-K");
    private final SecClient secClient;
    private final FilingService filingService;

    public SecFilingMonitor(SecClient secClient, FilingService filingService) {
        this.secClient = secClient;
        this.filingService = filingService;
    }

    @Override
    public void poll(Company company) {
        secClient.recentFilings(company).stream()
                .filter(this::isEarningsRelated)
                .forEach(metadata -> filingService.registerIfNew(company, metadata));
    }

    boolean isEarningsRelated(SecFilingMetadata filing) {
        String form = filing.formType().toUpperCase(Locale.ROOT);
        if (PERIODIC_FORMS.contains(form)) {
            return true;
        }
        return form.equals("8-K") && filing.items() != null && filing.items().contains("2.02");
    }
}

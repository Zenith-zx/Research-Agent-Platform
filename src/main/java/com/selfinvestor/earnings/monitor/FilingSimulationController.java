package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.CompanyRepository;
import com.selfinvestor.earnings.filing.FilingService;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(prefix = "earnings.simulation", name = "enabled", havingValue = "true")
public class FilingSimulationController {
    private final CompanyRepository companies;
    private final FilingService filings;
    public FilingSimulationController(CompanyRepository companies, FilingService filings) { this.companies = companies; this.filings = filings; }

    @PostMapping("/simulate-filing")
    public Map<String, Object> simulate(@Valid @RequestBody SimulateFilingRequest request) {
        var company = companies.findByTickerIgnoreCase(request.ticker())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.ticker()));
        var metadata = new SecFilingMetadata(request.accessionNumber(), request.formType(), request.filingDate(), Instant.now(),
                request.primaryDocument(), request.items(), request.secUrl());
        return filings.registerIfNew(company, metadata)
                .<Map<String, Object>>map(f -> Map.of("created", true, "filingId", f.getId()))
                .orElseGet(() -> Map.of("created", false, "reason", "duplicate accessionNumber"));
    }
    public record SimulateFilingRequest(@NotBlank String ticker, @NotBlank String accessionNumber, @NotBlank String formType,
                                        @NotNull LocalDate filingDate, @NotBlank String primaryDocument, String items, @NotBlank String secUrl) {}
}

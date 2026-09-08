package com.selfinvestor.earnings.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {
    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Company> enabledCompanies() {
        return repository.findAllByEnabledTrueOrderByTicker();
    }

    @Transactional(readOnly = true)
    public List<Company> allCompanies() {
        return repository.findAll();
    }

    @Transactional
    public Company add(String ticker, String cik, String companyName, boolean enabled) {
        String normalizedTicker = ticker.trim().toUpperCase();
        String normalizedCik = normalizeCik(cik);
        repository.findByTickerIgnoreCase(normalizedTicker).ifPresent(existing -> {
            throw new IllegalArgumentException("Ticker already exists: " + normalizedTicker);
        });
        return repository.save(new Company(normalizedTicker, normalizedCik, companyName.trim(), enabled));
    }

    @Transactional
    public Company setEnabled(Long id, boolean enabled) {
        Company company = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
        company.setEnabled(enabled);
        return company;
    }

    static String normalizeCik(String cik) {
        String digits = cik == null ? "" : cik.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 10) {
            throw new IllegalArgumentException("CIK must contain 1 to 10 digits");
        }
        return String.format("%010d", Long.parseLong(digits));
    }
}

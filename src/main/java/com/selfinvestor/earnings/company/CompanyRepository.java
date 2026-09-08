package com.selfinvestor.earnings.company;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByEnabledTrueOrderByTicker();
    Optional<Company> findByTickerIgnoreCase(String ticker);
}

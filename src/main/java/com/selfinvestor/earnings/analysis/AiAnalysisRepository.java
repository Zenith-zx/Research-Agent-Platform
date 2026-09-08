package com.selfinvestor.earnings.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByFilingId(Long filingId);
    @EntityGraph(attributePaths = {"filing", "filing.company"})
    Optional<AiAnalysis> findOneById(Long id);
}

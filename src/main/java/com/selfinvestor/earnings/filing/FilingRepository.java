package com.selfinvestor.earnings.filing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface FilingRepository extends JpaRepository<Filing, Long> {
    boolean existsByAccessionNumber(String accessionNumber);
    Optional<Filing> findByAccessionNumber(String accessionNumber);
    @EntityGraph(attributePaths = "company")
    Optional<Filing> findOneById(Long id);
    List<Filing> findAllByStatusIn(Collection<FilingStatus> statuses);
    @EntityGraph(attributePaths = "company")
    List<Filing> findAllByOrderByIdDesc();
}

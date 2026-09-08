package com.selfinvestor.earnings.filing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FilingDocumentRepository extends JpaRepository<FilingDocument, Long> {
    List<FilingDocument> findAllByFilingIdOrderById(Long filingId);
    List<FilingDocument> findAllByFilingIdAndSelectedForAnalysisTrue(Long filingId);
}

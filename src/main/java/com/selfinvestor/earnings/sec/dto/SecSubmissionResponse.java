package com.selfinvestor.earnings.sec.dto;

import java.util.List;

public record SecSubmissionResponse(Filings filings) {
    public record Filings(Recent recent) {}
    public record Recent(
            List<String> accessionNumber,
            List<String> filingDate,
            List<String> acceptanceDateTime,
            List<String> form,
            List<String> primaryDocument,
            List<String> items) {}
}

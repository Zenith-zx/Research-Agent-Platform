package com.selfinvestor.earnings.sec.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SecFilingMetadata(
        String accessionNumber,
        String formType,
        LocalDate filingDate,
        Instant acceptedAt,
        String primaryDocument,
        String items,
        String filingUrl) {}

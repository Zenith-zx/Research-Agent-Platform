package com.selfinvestor.earnings.filing;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class FilingDocumentSelector {
    public List<FilingDocument> select(List<FilingDocument> documents, String filingForm) {
        return documents.stream().filter(document -> document.getRawContent() != null && !document.getRawContent().isBlank())
                .sorted(Comparator.comparingInt((FilingDocument d) -> score(d, filingForm)).reversed())
                .limit(1).filter(document -> score(document, filingForm) > 0).toList();
    }

    int score(FilingDocument document, String filingForm) {
        String type = lower(document.getDocumentType());
        String description = lower(document.getDescription());
        String filename = lower(document.getDocumentName());
        int score = 0;
        if (type.equals("ex-99.1")) score += 1000;
        else if (type.startsWith("ex-99")) score += 800;
        if (containsFinancialResult(description)) score += 600;
        if (containsFinancialResult(filename)) score += 300;
        if (type.equals("10-q") || type.equals("10-k") || type.equals(lower(filingForm))) score += 200;
        if (filename.endsWith(".xsd") || filename.contains("cal.xml") || filename.contains("def.xml") || filename.contains("lab.xml") || filename.contains("pre.xml")) score -= 1000;
        return score;
    }
    private static boolean containsFinancialResult(String text) {
        return text.contains("earning") || text.contains("financial result") || text.contains("quarterly result") || text.contains("press release");
    }
    private static String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
}

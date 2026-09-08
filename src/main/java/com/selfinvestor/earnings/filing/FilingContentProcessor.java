package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.config.EarningsProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FilingContentProcessor {
    public enum ChunkType { COMPLETE_DOCUMENT, FINANCIAL_STATEMENTS, GUIDANCE, MANAGEMENT_COMMENTARY, OTHER }
    public record ContentChunk(ChunkType type, String content) {}
    private final int maxChars;

    public FilingContentProcessor(EarningsProperties properties) {
        this.maxChars = properties.getOpenai().getMaxInputChars();
    }

    public List<ContentChunk> process(List<FilingDocument> documents) {
        String combined = documents.stream().map(d -> "DOCUMENT: " + d.getDocumentName() + "\n" + d.getCleanContent())
                .reduce((a, b) -> a + "\n\n" + b).orElse("");
        if (combined.isBlank()) throw new IllegalArgumentException("No parsed filing content available");
        if (combined.length() <= maxChars) return List.of(new ContentChunk(ChunkType.COMPLETE_DOCUMENT, combined));

        Map<ChunkType, StringBuilder> sections = new LinkedHashMap<>();
        sections.put(ChunkType.FINANCIAL_STATEMENTS, new StringBuilder());
        sections.put(ChunkType.GUIDANCE, new StringBuilder());
        sections.put(ChunkType.MANAGEMENT_COMMENTARY, new StringBuilder());
        sections.put(ChunkType.OTHER, new StringBuilder());
        for (String paragraph : combined.split("\\n\\s*\\n|(?=\\n[A-Z][A-Z \\-/]{5,}:?\\n)")) {
            sections.get(classify(paragraph)).append(paragraph).append("\n\n");
        }
        List<ContentChunk> chunks = new ArrayList<>();
        sections.forEach((type, text) -> splitSemantically(type, text.toString(), chunks));
        return chunks;
    }

    private ChunkType classify(String paragraph) {
        String value = paragraph.toLowerCase(Locale.ROOT);
        if (contains(value, "guidance", "outlook", "expects", "forecast")) return ChunkType.GUIDANCE;
        if (contains(value, "chief executive", "chief financial", "management", "conference call", "commentary")) return ChunkType.MANAGEMENT_COMMENTARY;
        if (contains(value, "revenue", "gross margin", "operating income", "net income", "cash flow", "balance sheet", "earnings per share", "capex")) return ChunkType.FINANCIAL_STATEMENTS;
        return ChunkType.OTHER;
    }

    private void splitSemantically(ChunkType type, String text, List<ContentChunk> result) {
        if (text.isBlank()) return;
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > maxChars) {
                result.add(new ContentChunk(type, current.toString()));
                current.setLength(0);
            }
            if (paragraph.length() > maxChars) {
                for (String line : paragraph.split("\\n")) {
                    if (current.length() > 0 && current.length() + line.length() + 1 > maxChars) {
                        result.add(new ContentChunk(type, current.toString())); current.setLength(0);
                    }
                    if (line.length() <= maxChars) current.append(line).append('\n');
                    else for (int i = 0; i < line.length(); i += maxChars) result.add(new ContentChunk(type, line.substring(i, Math.min(i + maxChars, line.length()))));
                }
            } else current.append(paragraph).append("\n\n");
        }
        if (current.length() > 0) result.add(new ContentChunk(type, current.toString()));
    }

    private static boolean contains(String text, String... needles) {
        return Arrays.stream(needles).anyMatch(text::contains);
    }
}

package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FilingContentProcessor {
    /**
     * 财报内容分块类型。
     */
    @Getter
    @AllArgsConstructor
    public enum ChunkType {
        COMPLETE_DOCUMENT("完整文档"),
        FINANCIAL_STATEMENTS("财务报表"),
        GUIDANCE("业绩指引"),
        MANAGEMENT_COMMENTARY("管理层评论"),
        OTHER("其他内容");

        /**
         * 分块类型描述。
         */
        private final String desc;
    }

    /**
     * 发送给 AI 的财报内容分块。
     */
    @Data
    @AllArgsConstructor
    public static class ContentChunk {
        /**
         * 分块类型。
         */
        private ChunkType type;
        /**
         * 分块正文。
         */
        private String content;
    }
    @Resource
    private EarningsProperties properties;
    private int maxChars;

    /**
     * 初始化单个分块允许的最大字符数。
     */
    @PostConstruct
    void initialize() {
        maxChars = properties.getOpenai().getMaxInputChars();
    }

    /**
     * 将清洗后的财报文档整理为适合 AI 分析的语义分块。
     *
     * @param documents 已解析的 Filing 文档
     * @return 内容分块列表
     */
    public List<ContentChunk> process(List<FilingDocumentPO> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            throw new IllegalArgumentException("No parsed filing documents available");
        }
        String combined = documents.stream().map(d -> "DOCUMENT: " + d.getDocumentName() + "\n" + d.getCleanContent())
                .reduce((a, b) -> a + "\n\n" + b).orElse("");
        if (combined.isBlank()) {
            throw new IllegalArgumentException("No parsed filing content available");
        }
        if (combined.length() <= maxChars) {
            return Collections.singletonList(new ContentChunk(ChunkType.COMPLETE_DOCUMENT, combined));
        }

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

    /**
     * 根据关键词识别段落所属的语义分块。
     *
     * @param paragraph 财报段落
     * @return 分块类型
     */
    private ChunkType classify(String paragraph) {
        String value = paragraph.toLowerCase(Locale.ROOT);
        if (contains(value, "guidance", "outlook", "expects", "forecast")) {
            return ChunkType.GUIDANCE;
        }
        if (contains(value, "chief executive", "chief financial", "management", "conference call", "commentary")) {
            return ChunkType.MANAGEMENT_COMMENTARY;
        }
        if (contains(value, "revenue", "gross margin", "operating income", "net income", "cash flow",
                "balance sheet", "earnings per share", "capex")) {
            return ChunkType.FINANCIAL_STATEMENTS;
        }
        return ChunkType.OTHER;
    }

    /**
     * 在不截断段落语义的前提下拆分超长内容。
     *
     * @param type 分块类型
     * @param text 待拆分文本
     * @param result 分块结果容器
     */
    private void splitSemantically(ChunkType type, String text, List<ContentChunk> result) {
        if (text.isBlank()) {
            return;
        }
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > maxChars) {
                result.add(new ContentChunk(type, current.toString()));
                current.setLength(0);
            }
            if (paragraph.length() > maxChars) {
                for (String line : paragraph.split("\\n")) {
                    if (current.length() > 0 && current.length() + line.length() + 1 > maxChars) {
                        result.add(new ContentChunk(type, current.toString()));
                        current.setLength(0);
                    }
                    if (line.length() <= maxChars) {
                        current.append(line).append('\n');
                    } else {
                        for (int i = 0; i < line.length(); i += maxChars) {
                            result.add(new ContentChunk(type, line.substring(i, Math.min(i + maxChars, line.length()))));
                        }
                    }
                }
            } else {
                current.append(paragraph).append("\n\n");
            }
        }
        if (current.length() > 0) {
            result.add(new ContentChunk(type, current.toString()));
        }
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text 待匹配文本
     * @param needles 关键词集合
     * @return 是否包含关键词
     */
    private static boolean contains(String text, String... needles) {
        return Arrays.stream(needles).anyMatch(text::contains);
    }
}

package com.selfinvestor.earnings.filing;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class FilingDocumentSelector {
    /**
     * 按财报相关性选择最高价值的附件。
     *
     * @param documents Filing 附件列表
     * @param filingForm Filing 表单类型
     * @return 被选中的附件列表
     */
    public List<FilingDocumentPO> select(List<FilingDocumentPO> documents, String filingForm) {
        return documents.stream().filter(document -> document.getRawContent() != null && !document.getRawContent().isBlank())
                .sorted(Comparator.comparingInt((FilingDocumentPO document) -> score(document, filingForm)).reversed())
                .limit(1).filter(document -> score(document, filingForm) > 0).collect(Collectors.toList());
    }

    /**
     * 根据类型、描述和文件名为附件评分。
     *
     * @param document Filing 附件
     * @param filingForm Filing 表单类型
     * @return 相关性得分
     */
    int score(FilingDocumentPO document, String filingForm) {
        String type = lower(document.getDocumentType());
        String description = lower(document.getDescription());
        String filename = lower(document.getDocumentName());
        int score = 0;
        if (type.equals("ex-99.1")) {
            score += 1000;
        } else if (type.startsWith("ex-99")) {
            score += 800;
        }
        if (containsFinancialResult(description)) {
            score += 600;
        }
        if (containsFinancialResult(filename)) {
            score += 300;
        }
        if (type.equals("10-q") || type.equals("10-k") || type.equals(lower(filingForm))) {
            score += 200;
        }
        if (filename.endsWith(".xsd") || filename.contains("cal.xml") || filename.contains("def.xml")
                || filename.contains("lab.xml") || filename.contains("pre.xml")) {
            score -= 1000;
        }
        return score;
    }

    /**
     * 判断文本是否描述财务结果。
     *
     * @param text 待判断文本
     * @return 是否包含财务结果关键词
     */
    private static boolean containsFinancialResult(String text) {
        return text.contains("earning") || text.contains("financial result") || text.contains("quarterly result") || text.contains("press release");
    }

    /**
     * 安全转换为小写文本。
     *
     * @param value 原始文本
     * @return 小写文本
     */
    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

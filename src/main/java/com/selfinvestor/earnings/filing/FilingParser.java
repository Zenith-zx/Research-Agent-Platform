package com.selfinvestor.earnings.filing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class FilingParser {
    /**
     * 清洗 SEC HTML 并保留正文与表格内容。
     *
     * @param rawContent 原始 SEC 文档
     * @return 清洗后的文本
     */
    public String parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Filing content is empty");
        }
        Document document = Jsoup.parse(rawContent);
        document.select("script,style,noscript,nav,header,footer,svg").remove();
        for (Element table : document.select("table")) {
            StringBuilder preserved = new StringBuilder("\n");
            for (Element row : table.select("tr")) {
                org.jsoup.select.Elements cells = row.select("th,td");
                if (CollectionUtils.isNotEmpty(cells)) {
                    preserved.append(cells.stream().map(Element::text).map(String::trim)
                            .reduce((a, b) -> a + " | " + b).orElse("")).append('\n');
                }
            }
            table.replaceWith(new Element("pre").text(preserved.toString()));
        }
        document.select("p,div,h1,h2,h3,h4,li,pre").append("\n");
        String text = document.body().wholeText()
                .replace('\u00a0', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("No text remained after parsing filing HTML");
        }
        return text;
    }
}

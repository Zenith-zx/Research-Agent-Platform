package com.selfinvestor.earnings.filing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class FilingParser {
    public String parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) throw new IllegalArgumentException("Filing content is empty");
        Document document = Jsoup.parse(rawContent);
        document.select("script,style,noscript,nav,header,footer,svg").remove();
        for (Element table : document.select("table")) {
            StringBuilder preserved = new StringBuilder("\n");
            for (Element row : table.select("tr")) {
                var cells = row.select("th,td");
                if (!cells.isEmpty()) {
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
        if (text.isBlank()) throw new IllegalArgumentException("No text remained after parsing filing HTML");
        return text;
    }
}

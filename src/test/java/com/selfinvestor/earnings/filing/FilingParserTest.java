package com.selfinvestor.earnings.filing;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FilingParserTest {
    /**
     * 验证 HTML 噪声会被删除且财务表格数据会被保留。
     *
     * @throws IOException Fixture 读取失败时抛出
     */
    @Test
    void removesNoiseAndPreservesFinancialTable() throws IOException {
        String html = new String(getClass().getResourceAsStream("/fixtures/filing.html").readAllBytes(), StandardCharsets.UTF_8);
        String result = new FilingParser().parse(html);
        assertThat(result).contains("Revenue | 250 | 200", "Net income | 50 | 40", "Guidance");
        assertThat(result).doesNotContain("bad()", "SEC navigation");
    }
}

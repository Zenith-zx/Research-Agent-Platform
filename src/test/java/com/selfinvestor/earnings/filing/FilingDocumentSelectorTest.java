package com.selfinvestor.earnings.filing;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

class FilingDocumentSelectorTest {
    /**
     * 验证 EX-99.1 的选择优先级高于主文档和 XBRL 文件。
     */
    @Test
    void selectsExhibit991AheadOfMain8kAndXbrlFiles() {
        FilingDocumentPO main = new FilingDocumentPO(null, "8-K", "form8-k.htm", "Current report", "u", "main");
        FilingDocumentPO xbrl = new FilingDocumentPO(null, "XML", "report_pre.xml", "XBRL", "u", "xml");
        FilingDocumentPO release = new FilingDocumentPO(null, "EX-99.1", "earnings-release.htm", "Earnings Release", "u", "release");
        List<FilingDocumentPO> selected = new FilingDocumentSelector().select(Arrays.asList(main, xbrl, release), "8-K");
        assertThat(selected).containsExactly(release);
    }
}

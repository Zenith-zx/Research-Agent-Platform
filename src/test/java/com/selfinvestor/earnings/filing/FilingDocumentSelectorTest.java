package com.selfinvestor.earnings.filing;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FilingDocumentSelectorTest {
    @Test
    void selectsExhibit991AheadOfMain8kAndXbrlFiles() {
        FilingDocument main = new FilingDocument(null, "8-K", "form8-k.htm", "Current report", "u", "main");
        FilingDocument xbrl = new FilingDocument(null, "XML", "report_pre.xml", "XBRL", "u", "xml");
        FilingDocument release = new FilingDocument(null, "EX-99.1", "earnings-release.htm", "Earnings Release", "u", "release");
        List<FilingDocument> selected = new FilingDocumentSelector().select(List.of(main, xbrl, release), "8-K");
        assertThat(selected).containsExactly(release);
    }
}

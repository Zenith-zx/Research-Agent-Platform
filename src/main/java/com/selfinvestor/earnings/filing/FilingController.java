package com.selfinvestor.earnings.filing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/filings")
public class FilingController {
    private final FilingRepository filings;
    public FilingController(FilingRepository filings) { this.filings = filings; }

    @GetMapping
    public List<FilingResponse> list() {
        return filings.findAllByOrderByIdDesc().stream().map(f -> new FilingResponse(f.getId(), f.getCompany().getTicker(),
                f.getAccessionNumber(), f.getFormType(), f.getFilingDate().toString(), f.getStatus(), f.getSecUrl(), f.getErrorMessage())).toList();
    }
    public record FilingResponse(Long id, String ticker, String accessionNumber, String formType, String filingDate,
                                 FilingStatus status, String secUrl, String errorMessage) {}
}

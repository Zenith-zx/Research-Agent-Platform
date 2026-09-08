package com.selfinvestor.earnings.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filings/{filingId}/analysis")
public class AnalysisController {
    private final AiAnalysisRepository analyses;
    private final ObjectMapper mapper;
    public AnalysisController(AiAnalysisRepository analyses, ObjectMapper mapper) { this.analyses = analyses; this.mapper = mapper; }

    @GetMapping
    public JsonNode get(@PathVariable Long filingId) throws Exception {
        AiAnalysis analysis = analyses.findByFilingId(filingId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found for filing: " + filingId));
        return mapper.readTree(analysis.getAnalysisJson());
    }
}

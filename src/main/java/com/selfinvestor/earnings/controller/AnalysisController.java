package com.selfinvestor.earnings.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.service.AiAnalysisService;
import com.selfinvestor.earnings.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 财报分析查询接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/filings/{filingId}/analysis")
public class AnalysisController {
    @Resource
    private AiAnalysisService aiAnalysisService;

    /**
     * 查询指定 Filing 的结构化分析结果。
     *
     * @param filingId Filing 主键
     * @return 结构化分析 JSON
     */
    @GetMapping
    public JsonNode get(@PathVariable Long filingId) {
        try {
            return aiAnalysisService.findAnalysis(filingId);
        } catch (BusinessException exception) {
            log.warn("Query analysis failed: filingId={} error={}", filingId, exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Query analysis failed unexpectedly: filingId={}", filingId, exception);
            throw exception;
        }
    }
}

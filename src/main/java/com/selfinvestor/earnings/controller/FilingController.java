package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.common.exception.BusinessException;
import com.selfinvestor.earnings.controller.dto.FilingResponseDTO;
import com.selfinvestor.earnings.service.FilingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filing 查询接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/filings")
public class FilingController {
    @Resource
    private FilingService filingService;

    /**
     * 按主键倒序查询 Filing。
     *
     * @return Filing 响应列表
     */
    @GetMapping
    public List<FilingResponseDTO> list() {
        try {
            return filingService.findAll().stream().map(FilingResponseDTO::from).collect(Collectors.toList());
        } catch (BusinessException exception) {
            log.warn("Query filings failed: {}", exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Query filings failed unexpectedly", exception);
            throw exception;
        }
    }
}

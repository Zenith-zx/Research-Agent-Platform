package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.common.exception.BusinessException;
import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.service.CompanyService;
import com.selfinvestor.earnings.controller.dto.SimulateFilingRequestDTO;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.service.FilingService;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 本地 Filing 模拟接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(prefix = "earnings.simulation", name = "enabled", havingValue = "true")
public class FilingSimulationController {
    @Resource
    private CompanyService companyService;
    @Resource
    private FilingService filingService;

    /**
     * 模拟发现一份新 Filing。
     *
     * @param request 模拟 Filing 参数
     * @return 创建结果
     */
    @PostMapping("/simulate-filing")
    public Map<String, Object> simulate(@Valid @RequestBody SimulateFilingRequestDTO request) {
        try {
            CompanyPO company = companyService.findByTicker(request.getTicker());
            SecFilingMetadataDTO metadata = new SecFilingMetadataDTO(request.getAccessionNumber(), request.getFormType(),
                    request.getFilingDate(), Instant.now(), request.getPrimaryDocument(), request.getItems(), request.getSecUrl());
            Map<String, Object> response = new HashMap<>();
            Optional<FilingPO> created = filingService.registerIfNew(company, metadata);
            if (created.isPresent()) {
                response.put("created", true);
                response.put("filingId", created.get().getId());
            } else {
                response.put("created", false);
                response.put("reason", "duplicate accessionNumber");
            }
            return response;
        } catch (BusinessException exception) {
            log.warn("Simulate filing failed: ticker={} accession={} error={}", request.getTicker(),
                    request.getAccessionNumber(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Simulate filing failed unexpectedly: ticker={} accession={}", request.getTicker(),
                    request.getAccessionNumber(), exception);
            throw exception;
        }
    }
}

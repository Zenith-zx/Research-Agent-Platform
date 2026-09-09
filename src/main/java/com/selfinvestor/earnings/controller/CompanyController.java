package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.common.exception.BusinessException;
import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.service.CompanyService;
import com.selfinvestor.earnings.controller.dto.CompanyResponseDTO;
import com.selfinvestor.earnings.controller.dto.CreateCompanyRequestDTO;
import com.selfinvestor.earnings.controller.dto.EnabledRequestDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公司关注列表接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    @Resource
    private CompanyService service;

    /**
     * 查询全部关注公司。
     *
     * @return 公司列表
     */
    @GetMapping
    public List<CompanyResponseDTO> list() {
        try {
            return service.allCompanies().stream().map(CompanyResponseDTO::from).collect(Collectors.toList());
        } catch (BusinessException exception) {
            log.warn("Query companies failed: {}", exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Query companies failed unexpectedly", exception);
            throw exception;
        }
    }

    /**
     * 添加关注公司。
     *
     * @param request 公司创建参数
     * @return 新增的公司
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponseDTO add(@Valid @RequestBody CreateCompanyRequestDTO request) {
        try {
            CompanyPO company = service.add(request.getTicker(), request.getCik(), request.getCompanyName(), request.isEnabled());
            return CompanyResponseDTO.from(company);
        } catch (BusinessException exception) {
            log.warn("Create company failed: ticker={} error={}", request.getTicker(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Create company failed unexpectedly: ticker={}", request.getTicker(), exception);
            throw exception;
        }
    }

    /**
     * 修改公司的监听状态。
     *
     * @param id 公司主键
     * @param request 状态修改参数
     * @return 修改后的公司
     */
    @PatchMapping("/{id}/enabled")
    public CompanyResponseDTO setEnabled(@PathVariable Long id, @RequestBody EnabledRequestDTO request) {
        try {
            return CompanyResponseDTO.from(service.setEnabled(id, request.isEnabled()));
        } catch (BusinessException exception) {
            log.warn("Update company status failed: companyId={} error={}", id, exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Update company status failed unexpectedly: companyId={}", id, exception);
            throw exception;
        }
    }
}

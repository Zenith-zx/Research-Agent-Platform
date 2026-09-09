package com.selfinvestor.earnings.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 模拟 Filing 请求 DTO。
 */
@Data
@NoArgsConstructor
public class SimulateFilingRequestDTO {
    /**
     * 股票代码。
     */
    @NotBlank
    private String ticker;
    /**
     * SEC accession number。
     */
    @NotBlank
    private String accessionNumber;
    /**
     * 申报表单类型。
     */
    @NotBlank
    private String formType;
    /**
     * 申报日期。
     */
    @NotNull
    private LocalDate filingDate;
    /**
     * 主文档文件名。
     */
    @NotBlank
    private String primaryDocument;
    /**
     * 8-K 条目。
     */
    private String items;
    /**
     * SEC 页面地址。
     */
    @NotBlank
    private String secUrl;
}

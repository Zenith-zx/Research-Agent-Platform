package com.selfinvestor.earnings.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增公司请求 DTO。
 */
@Data
@NoArgsConstructor
public class CreateCompanyRequestDTO {
    /**
     * 股票代码。
     */
    @NotBlank
    @Pattern(regexp = "[A-Za-z.\\-]{1,16}")
    private String ticker;
    /**
     * SEC CIK。
     */
    @NotBlank
    @Pattern(regexp = "\\d{1,10}")
    private String cik;
    /**
     * 公司名称。
     */
    @NotBlank
    private String companyName;
    /**
     * 是否启用监听。
     */
    private boolean enabled;
}

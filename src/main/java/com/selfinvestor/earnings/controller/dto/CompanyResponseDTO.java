package com.selfinvestor.earnings.controller.dto;

import com.selfinvestor.earnings.company.CompanyPO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司响应 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDTO {
    /**
     * 公司主键。
     */
    private Long id;
    /**
     * 股票代码。
     */
    private String ticker;
    /**
     * SEC CIK。
     */
    private String cik;
    /**
     * 公司名称。
     */
    private String companyName;
    /**
     * 是否启用监听。
     */
    private boolean enabled;

    /**
     * 将公司实体转换为响应 DTO。
     *
     * @param company 公司实体
     * @return 公司响应 DTO
     */
    public static CompanyResponseDTO from(CompanyPO company) {
        return new CompanyResponseDTO(company.getId(), company.getTicker(), company.getCik(),
                company.getCompanyName(), company.isEnabled());
    }
}

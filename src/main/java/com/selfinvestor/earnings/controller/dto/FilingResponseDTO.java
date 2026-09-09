package com.selfinvestor.earnings.controller.dto;

import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.enums.FilingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filing 查询响应 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilingResponseDTO {
    /**
     * Filing 主键。
     */
    private Long id;
    /**
     * 股票代码。
     */
    private String ticker;
    /**
     * SEC accession number。
     */
    private String accessionNumber;
    /**
     * 申报表单类型。
     */
    private String formType;
    /**
     * 申报日期。
     */
    private String filingDate;
    /**
     * 处理状态。
     */
    private FilingStatus status;
    /**
     * SEC 页面地址。
     */
    private String secUrl;
    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 将 Filing 实体转换为响应 DTO。
     *
     * @param filing Filing 实体
     * @return Filing 响应 DTO
     */
    public static FilingResponseDTO from(FilingPO filing) {
        return new FilingResponseDTO(filing.getId(), filing.getCompany().getTicker(), filing.getAccessionNumber(),
                filing.getFormType(), filing.getFilingDate().toString(), filing.getStatus(), filing.getSecUrl(),
                filing.getErrorMessage());
    }
}

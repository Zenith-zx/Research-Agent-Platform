package com.selfinvestor.earnings.sec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * SEC Filing 元数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecFilingMetadataDTO {
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
    private LocalDate filingDate;
    /**
     * SEC 接收时间。
     */
    private Instant acceptedAt;
    /**
     * 主文档文件名。
     */
    private String primaryDocument;
    /**
     * 8-K 条目列表。
     */
    private String items;
    /**
     * Filing 地址。
     */
    private String filingUrl;
}

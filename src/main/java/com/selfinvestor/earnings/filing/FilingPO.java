package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.enums.FilingStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * SEC Filing 持久化实体。
 */
@Data
@NoArgsConstructor
public class FilingPO {
    /**
     * Filing 主键。
     */
    private Long id;
    /**
     * 关联公司。
     */
    private CompanyPO company;
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
     * SEC 页面地址。
     */
    private String secUrl;
    /**
     * 处理状态。
     */
    private FilingStatus status;
    /**
     * 失败原因。
     */
    private String errorMessage;
    /**
     * 创建时间。
     */
    private Instant createdAt;
    /**
     * 更新时间。
     */
    private Instant updatedAt;

    /**
     * 创建待处理 Filing。
     *
     * @param company 关联公司
     * @param accessionNumber SEC accession number
     * @param formType 表单类型
     * @param filingDate 申报日期
     * @param acceptedAt SEC 接收时间
     * @param primaryDocument 主文档文件名
     * @param secUrl SEC 页面地址
     */
    public FilingPO(CompanyPO company, String accessionNumber, String formType, LocalDate filingDate,
                    Instant acceptedAt, String primaryDocument, String secUrl) {
        this.company = company;
        this.accessionNumber = accessionNumber;
        this.formType = formType;
        this.filingDate = filingDate;
        this.acceptedAt = acceptedAt;
        this.primaryDocument = primaryDocument;
        this.secUrl = secUrl;
        this.status = FilingStatus.DETECTED;
    }

    /**
     * 切换 Filing 处理状态并清空错误信息。
     *
     * @param status 新状态
     */
    public void transitionTo(FilingStatus status) {
        this.status = status;
        this.errorMessage = null;
    }

    /**
     * 将 Filing 标记为失败。
     *
     * @param message 失败原因
     */
    public void fail(String message) {
        this.status = FilingStatus.FAILED;
        this.errorMessage = message == null ? "Unknown error" : message.substring(0, Math.min(2000, message.length()));
    }
}

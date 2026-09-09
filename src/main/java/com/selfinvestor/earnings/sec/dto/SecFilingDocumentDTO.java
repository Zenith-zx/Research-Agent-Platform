package com.selfinvestor.earnings.sec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SEC Filing 附件内容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecFilingDocumentDTO {
    /**
     * 文档类型。
     */
    private String type;
    /**
     * 文件名。
     */
    private String filename;
    /**
     * 文档描述。
     */
    private String description;
    /**
     * SEC 文档地址。
     */
    private String url;
    /**
     * 原始文档内容。
     */
    private String content;
}

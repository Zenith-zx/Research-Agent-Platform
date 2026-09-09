package com.selfinvestor.earnings.filing;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filing 附件持久化实体。
 */
@Data
@NoArgsConstructor
public class FilingDocumentPO {
    /**
     * 附件主键。
     */
    private Long id;
    /**
     * 关联 Filing。
     */
    private FilingPO filing;
    /**
     * 文档类型。
     */
    private String documentType;
    /**
     * 文档文件名。
     */
    private String documentName;
    /**
     * 文档描述。
     */
    private String description;
    /**
     * 文档地址。
     */
    private String url;
    /**
     * 原始内容。
     */
    private String rawContent;
    /**
     * 清洗后内容。
     */
    private String cleanContent;
    /**
     * 是否被选中用于分析。
     */
    private boolean selectedForAnalysis;

    /**
     * 创建 Filing 附件。
     *
     * @param filing 关联 Filing
     * @param documentType 文档类型
     * @param documentName 文件名
     * @param description 文档描述
     * @param url 文档地址
     * @param rawContent 原始内容
     */
    public FilingDocumentPO(FilingPO filing, String documentType, String documentName, String description, String url, String rawContent) {
        this.filing = filing; this.documentType = documentType; this.documentName = documentName;
        this.description = description; this.url = url; this.rawContent = rawContent;
    }
    /**
     * 保存附件解析结果。
     *
     * @param cleanContent 清洗后内容
     * @param selected 是否用于 AI 分析
     */
    public void parsed(String cleanContent, boolean selected) {
        this.cleanContent = cleanContent;
        this.selectedForAnalysis = selected;
    }
}

package com.selfinvestor.earnings.sec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SEC submissions 接口响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecSubmissionResponseDTO {
    /**
     * 公司申报信息。
     */
    private FilingsDTO filings;

    /**
     * Filing 数据容器。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilingsDTO {
        /**
         * 最近申报列表。
         */
        private RecentDTO recent;
    }

    /**
     * 最近 Filing 字段列表。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentDTO {
        /**
         * Accession number 列表。
         */
        private List<String> accessionNumber;
        /**
         * 申报日期列表。
         */
        private List<String> filingDate;
        /**
         * SEC 接收时间列表。
         */
        private List<String> acceptanceDateTime;
        /**
         * 表单类型列表。
         */
        private List<String> form;
        /**
         * 主文档文件名列表。
         */
        private List<String> primaryDocument;
        /**
         * 8-K 条目列表。
         */
        private List<String> items;
    }
}

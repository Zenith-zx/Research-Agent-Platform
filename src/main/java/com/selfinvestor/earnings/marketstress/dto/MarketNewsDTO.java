package com.selfinvestor.earnings.marketstress.dto;

import com.selfinvestor.earnings.enums.NewsCategory;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 市场新闻 DTO。
 */
@Data
@NoArgsConstructor
public class MarketNewsDTO {
    /**
     * 新闻标题。
     */
    private String title;
    /**
     * 新闻来源。
     */
    private String source;
    /**
     * 发布时间。
     */
    private Instant publishedAt;
    /**
     * 摘要或片段。
     */
    private String summary;
    /**
     * 原文地址。
     */
    private String url;
    /**
     * 新闻类别。
     */
    private NewsCategory category;
}

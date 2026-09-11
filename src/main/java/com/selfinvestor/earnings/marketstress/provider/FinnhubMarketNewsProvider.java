package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.enums.NewsCategory;
import com.selfinvestor.earnings.marketstress.dto.MarketNewsDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Finnhub 市场新闻供应商。
 */
@Component
@Slf4j
public class FinnhubMarketNewsProvider implements MarketNewsProvider {
    @Resource
    private FinnhubClient client;

    /**
     * 获取、分类、去重并限制最近市场新闻。
     *
     * @param lookback 回溯时间
     * @param limit 最大新闻数量
     * @return 按时间倒序排列的市场新闻
     */
    @Override
    public List<MarketNewsDTO> getRecentMarketNews(Duration lookback, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }
        Instant cutoff = Instant.now().minus(lookback);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("category", "general");
        parameters.put("minId", 0);
        JsonNode response = client.get("/news", parameters);
        if (!response.isArray()) {
            log.warn("Market news unavailable from Finnhub");
            return new ArrayList<>();
        }
        List<MarketNewsDTO> news = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (JsonNode item : response) {
            MarketNewsDTO article = map(item);
            if (article.getPublishedAt() == null || article.getPublishedAt().isBefore(cutoff)) {
                continue;
            }
            String key = article.getUrl() == null || article.getUrl().isBlank()
                    ? article.getTitle().toLowerCase(Locale.ROOT) : article.getUrl();
            if (keys.add(key)) {
                news.add(article);
            }
        }
        news.sort(Comparator.comparing(MarketNewsDTO::getPublishedAt).reversed());
        if (news.size() > limit) {
            return new ArrayList<>(news.subList(0, limit));
        }
        return news;
    }

    /**
     * 将 Finnhub 新闻映射为统一 DTO。
     *
     * @param item 新闻 JSON
     * @return 新闻 DTO
     */
    private MarketNewsDTO map(JsonNode item) {
        MarketNewsDTO article = new MarketNewsDTO();
        article.setTitle(item.path("headline").asText(""));
        article.setSource(item.path("source").asText("Finnhub"));
        long timestamp = item.path("datetime").asLong(0L);
        article.setPublishedAt(timestamp > 0 ? Instant.ofEpochSecond(timestamp) : null);
        article.setSummary(item.path("summary").asText(""));
        article.setUrl(item.path("url").asText(""));
        article.setCategory(classify(article.getTitle() + " " + article.getSummary()));
        return article;
    }

    /**
     * 按关键词对市场新闻进行轻量分类。
     *
     * @param text 标题与摘要
     * @return 新闻类别
     */
    private NewsCategory classify(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "federal reserve", "fed ", "fomc", "powell")) {
            return NewsCategory.FEDERAL_RESERVE;
        }
        if (containsAny(normalized, "treasury", "bond yield")) {
            return NewsCategory.US_TREASURY;
        }
        if (containsAny(normalized, "tariff", "trade war")) {
            return NewsCategory.TARIFFS;
        }
        if (containsAny(normalized, "inflation", "cpi", "pce")) {
            return NewsCategory.INFLATION;
        }
        if (containsAny(normalized, "jobs", "employment", "payroll", "unemployment")) {
            return NewsCategory.EMPLOYMENT;
        }
        if (containsAny(normalized, "recession", "contraction")) {
            return NewsCategory.RECESSION;
        }
        if (containsAny(normalized, "middle east", "israel", "iran", "gaza")) {
            return NewsCategory.MIDDLE_EAST;
        }
        if (containsAny(normalized, "oil", "brent", "wti", "opec")) {
            return NewsCategory.OIL;
        }
        if (containsAny(normalized, "war", "geopolitical", "conflict")) {
            return NewsCategory.GEOPOLITICAL_CONFLICT;
        }
        if (containsAny(normalized, "bank", "credit", "funding", "default")) {
            return NewsCategory.BANKING_CREDIT;
        }
        if (containsAny(normalized, "semiconductor", "chip", "artificial intelligence", " ai ")) {
            return NewsCategory.AI_SEMICONDUCTOR;
        }
        if (containsAny(normalized, "earnings", "profit", "revenue")) {
            return NewsCategory.MAJOR_EARNINGS;
        }
        if (containsAny(normalized, "shutdown")) {
            return NewsCategory.GOVERNMENT_SHUTDOWN;
        }
        if (containsAny(normalized, "white house", "president")) {
            return NewsCategory.WHITE_HOUSE;
        }
        if (containsAny(normalized, "fiscal", "budget", "government spending")) {
            return NewsCategory.FISCAL_POLICY;
        }
        return NewsCategory.OTHER;
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text 文本
     * @param keywords 关键词
     * @return 是否包含
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

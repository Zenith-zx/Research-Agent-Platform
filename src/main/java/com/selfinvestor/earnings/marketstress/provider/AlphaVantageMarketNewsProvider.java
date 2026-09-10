package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.enums.NewsCategory;
import com.selfinvestor.earnings.marketstress.dto.MarketNewsDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Alpha Vantage 市场新闻供应商。
 */
@Component
@Slf4j
public class AlphaVantageMarketNewsProvider implements MarketNewsProvider {
    private static final DateTimeFormatter REQUEST_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter RESPONSE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);

    @Resource
    private AlphaVantageClient client;

    /**
     * 获取、分类、去重并限制市场新闻数量。
     *
     * @param lookback 回溯时间
     * @param limit 最大新闻数量
     * @return 市场新闻
     */
    @Override
    public List<MarketNewsDTO> getRecentMarketNews(Duration lookback, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }
        Instant cutoff = Instant.now().minus(lookback);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("function", "NEWS_SENTIMENT");
        parameters.put("topics", "financial_markets");
        parameters.put("time_from", REQUEST_TIME.format(cutoff));
        parameters.put("sort", "LATEST");
        parameters.put("limit", Integer.toString(Math.min(1000, Math.max(limit * 3, limit))));
        JsonNode response = client.get(parameters);
        JsonNode feed = response.path("feed");
        if (!feed.isArray()) {
            log.warn("Market news unavailable from Alpha Vantage");
            return new ArrayList<>();
        }
        List<MarketNewsDTO> news = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (JsonNode item : feed) {
            try {
                MarketNewsDTO article = map(item);
                String key = article.getUrl() == null || article.getUrl().isBlank()
                        ? article.getTitle().toLowerCase(Locale.ROOT) : article.getUrl();
                if (!article.getPublishedAt().isBefore(cutoff) && keys.add(key)) {
                    news.add(article);
                }
            } catch (RuntimeException exception) {
                log.warn("Ignoring malformed market news item: reason={}", exception.getMessage());
            }
        }
        news.sort(Comparator.comparing(MarketNewsDTO::getPublishedAt).reversed());
        if (news.size() > limit) {
            return new ArrayList<>(news.subList(0, limit));
        }
        return news;
    }

    /**
     * 将供应商新闻映射为统一 DTO。
     *
     * @param item 新闻 JSON
     * @return 新闻 DTO
     */
    private MarketNewsDTO map(JsonNode item) {
        MarketNewsDTO article = new MarketNewsDTO();
        article.setTitle(item.path("title").asText(""));
        article.setSource(item.path("source").asText("Alpha Vantage"));
        article.setPublishedAt(Instant.from(RESPONSE_TIME.parse(item.path("time_published").asText())));
        article.setSummary(item.path("summary").asText(""));
        article.setUrl(item.path("url").asText(""));
        article.setCategory(classify(article.getTitle() + " " + article.getSummary()));
        return article;
    }

    /**
     * 按关键词对重要市场新闻进行轻量分类。
     *
     * @param text 标题和摘要
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

package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.service.DailyMarketCloseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finnhub 免费 Quote API 行情供应商。
 */
@Component
@Slf4j
public class FinnhubMarketDataProvider implements MarketDataProvider {
    private static final String SOURCE = "Finnhub Quote API";

    @Resource
    private FinnhubClient client;
    @Resource
    private DailyMarketCloseService dailyMarketCloseService;
    @Resource
    private MarketStressProperties properties;

    /**
     * 获取美股或 ETF 的最新报价与上一交易日正式收盘价。
     *
     * @param symbol 标的代码
     * @return 最新报价
     */
    @Override
    public MarketQuoteDTO getQuote(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        JsonNode response = requestQuote(normalizedSymbol);
        BigDecimal currentPrice = decimal(response, "c");
        if (currentPrice == null || currentPrice.signum() <= 0) {
            throw new IllegalStateException("Finnhub returned no current price for " + normalizedSymbol);
        }
        MarketQuoteDTO quote = new MarketQuoteDTO();
        quote.setSymbol(normalizedSymbol);
        quote.setPrice(currentPrice);
        quote.setDailyReturn(decimal(response, "dp"));
        quote.setPreviousClose(decimal(response, "pc"));
        long timestamp = response.path("t").asLong(0L);
        quote.setTimestamp(timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now());
        quote.setSource(SOURCE);
        return quote;
    }

    /**
     * 从本地 MySQL 每日收盘价表读取历史日线，不调用 Finnhub Premium Candle API。
     *
     * @param symbol 标的代码
     * @param days 最大交易日数量
     * @return 按交易日期升序排列的日线
     */
    @Override
    public List<DailyBarDTO> getDailyBars(String symbol, int days) {
        return dailyMarketCloseService.findLatestBars(normalizeSymbol(symbol), days);
    }

    /**
     * 获取可由 Finnhub Quote API 直接识别的指标，第一版仅尝试 VIX。
     *
     * @param type 指标类型
     * @return 指标值，不支持时返回空值 DTO
     */
    @Override
    public MarketMetricDTO getMetric(MarketMetricType type) {
        if (type != MarketMetricType.VIX) {
            return unavailable(type, null);
        }
        try {
            String symbol = properties.getProvider().getVixSymbol();
            JsonNode response = requestQuote(symbol);
            BigDecimal currentValue = decimal(response, "c");
            if (currentValue == null || currentValue.signum() <= 0) {
                return unavailable(type, "index points; change=points");
            }
            return new MarketMetricDTO(type.name(), currentValue, decimal(response, "d"),
                    "index points; change=points", quoteTimestamp(response), SOURCE);
        } catch (RuntimeException exception) {
            log.warn("Finnhub VIX unavailable: reason={}", exception.getMessage());
            return unavailable(type, "index points; change=points");
        }
    }

    /**
     * 调用 Finnhub Quote API。
     *
     * @param symbol 标的代码
     * @return Quote JSON
     */
    private JsonNode requestQuote(String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        return client.get("/quote", parameters);
    }

    /**
     * 创建供应商不支持的空值指标。
     *
     * @param type 指标类型
     * @param unit 单位
     * @return 空值指标
     */
    private MarketMetricDTO unavailable(MarketMetricType type, String unit) {
        return new MarketMetricDTO(type.name(), null, null, unit, null, SOURCE);
    }

    /**
     * 读取 Quote 响应中的十进制数值。
     *
     * @param response Quote JSON
     * @param field 字段名
     * @return 数值，缺失时返回 null
     */
    private BigDecimal decimal(JsonNode response, String field) {
        JsonNode value = response.path(field);
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.asText().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 读取 Quote 数据时间。
     *
     * @param response Quote JSON
     * @return UTC 时间戳
     */
    private Instant quoteTimestamp(JsonNode response) {
        long timestamp = response.path("t").asLong(0L);
        return timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now();
    }

    /**
     * 校验并统一标的代码。
     *
     * @param symbol 原始代码
     * @return 大写代码
     */
    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}

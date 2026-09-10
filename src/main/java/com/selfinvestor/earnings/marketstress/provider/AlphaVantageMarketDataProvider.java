package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Alpha Vantage 行情供应商。
 */
@Component
@Slf4j
public class AlphaVantageMarketDataProvider implements MarketDataProvider {
    private static final String SOURCE = "Alpha Vantage";
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Resource
    private AlphaVantageClient client;

    /**
     * 使用最近两个交易日计算最新价格和日收益率。
     *
     * @param symbol 标的代码
     * @return 最新行情
     */
    @Override
    public MarketQuoteDTO getQuote(String symbol) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("function", "GLOBAL_QUOTE");
        parameters.put("symbol", symbol);
        JsonNode response = client.get(parameters);
        JsonNode value = response.path("Global Quote");
        if (!value.isObject() || value.path("05. price").asText().isBlank()) {
            throw providerError("latest quote for " + symbol, response);
        }
        MarketQuoteDTO quote = new MarketQuoteDTO();
        quote.setSymbol(symbol.toUpperCase());
        quote.setPrice(new BigDecimal(value.path("05. price").asText()));
        String percent = value.path("10. change percent").asText().replace("%", "");
        quote.setDailyReturn(percent.isBlank() ? null : new BigDecimal(percent));
        String tradingDay = value.path("07. latest trading day").asText();
        quote.setTimestamp(tradingDay.isBlank() ? Instant.now()
                : LocalDate.parse(tradingDay).atStartOfDay(NEW_YORK).toInstant());
        quote.setSource(SOURCE);
        return quote;
    }

    /**
     * 获取 Alpha Vantage 日线行情。
     *
     * @param symbol 标的代码
     * @param days 最大交易日数量
     * @return 按日期升序排列的日线行情
     */
    @Override
    public List<DailyBarDTO> getDailyBars(String symbol, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be greater than zero");
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("function", "TIME_SERIES_DAILY");
        parameters.put("symbol", symbol);
        parameters.put("outputsize", "compact");
        JsonNode response = client.get(parameters);
        JsonNode series = response.path("Time Series (Daily)");
        if (!series.isObject()) {
            throw providerError("daily bars for " + symbol, response);
        }
        List<DailyBarDTO> bars = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = series.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            LocalDate date = LocalDate.parse(field.getKey());
            JsonNode value = field.getValue();
            bars.add(new DailyBarDTO(symbol.toUpperCase(), date, decimal(value, "1. open"), decimal(value, "2. high"),
                    decimal(value, "3. low"), decimal(value, "4. close"), decimal(value, "5. volume"),
                    date.atStartOfDay(NEW_YORK).toInstant(), SOURCE));
        }
        bars.sort(Comparator.comparing(DailyBarDTO::getDate));
        if (bars.size() > days) {
            return new ArrayList<>(bars.subList(bars.size() - days, bars.size()));
        }
        return bars;
    }

    /**
     * 获取跨资产指标，单项失败时返回空值指标。
     *
     * @param type 指标类型
     * @return 市场指标
     */
    @Override
    public MarketMetricDTO getMetric(MarketMetricType type) {
        if (type == MarketMetricType.DXY) {
            return unavailable(type, "Alpha Vantage does not provide a direct DXY series");
        }
        try {
            Map<String, String> parameters = metricParameters(type);
            JsonNode response = client.get(parameters);
            JsonNode data = response.path("data");
            if (!data.isArray() || data.size() == 0) {
                throw providerError(type.name(), response);
            }
            JsonNode latest = firstValid(data, 0);
            JsonNode previous = firstValid(data, 1);
            BigDecimal currentValue = metricValue(latest);
            BigDecimal previousValue = metricValue(previous);
            BigDecimal change = change(type, currentValue, previousValue);
            return new MarketMetricDTO(type.name(), currentValue, change, unit(type), metricTimestamp(latest), SOURCE);
        } catch (RuntimeException exception) {
            return unavailable(type, exception.getMessage());
        }
    }

    /**
     * 构建不同指标的供应商参数。
     *
     * @param type 指标类型
     * @return 查询参数
     */
    private Map<String, String> metricParameters(MarketMetricType type) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (type == MarketMetricType.VIX) {
            parameters.put("function", "INDEX_DATA");
            parameters.put("symbol", "VIX");
            parameters.put("interval", "daily");
            return parameters;
        }
        if (type == MarketMetricType.US2Y || type == MarketMetricType.US10Y || type == MarketMetricType.US30Y) {
            parameters.put("function", "TREASURY_YIELD");
            parameters.put("interval", "daily");
            parameters.put("maturity", type == MarketMetricType.US2Y ? "2year" :
                    type == MarketMetricType.US10Y ? "10year" : "30year");
            return parameters;
        }
        if (type == MarketMetricType.GOLD) {
            parameters.put("function", "GOLD_SILVER_HISTORY");
            parameters.put("symbol", "GOLD");
            parameters.put("interval", "daily");
            return parameters;
        }
        parameters.put("function", type.name());
        parameters.put("interval", "daily");
        return parameters;
    }

    /**
     * 读取数组中第一个有效数值点。
     *
     * @param data 数据数组
     * @param skip 需要跳过的有效点数
     * @return 数据点
     */
    private JsonNode firstValid(JsonNode data, int skip) {
        int found = 0;
        for (JsonNode item : data) {
            if (metricValue(item) != null) {
                if (found == skip) {
                    return item;
                }
                found++;
            }
        }
        return null;
    }

    /**
     * 从不同供应商格式读取指标值。
     *
     * @param node 数据点
     * @return 数值
     */
    private BigDecimal metricValue(JsonNode node) {
        if (node == null) {
            return null;
        }
        String value = node.path("value").asText();
        if (value.isBlank()) {
            value = node.path("close").asText();
        }
        if (value.isBlank() || ".".equals(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    /**
     * 读取指标数据时间。
     *
     * @param node 数据点
     * @return UTC 时间戳
     */
    private Instant metricTimestamp(JsonNode node) {
        if (node == null) {
            return null;
        }
        String date = node.path("date").asText();
        if (date.isBlank()) {
            date = node.path("timestamp").asText();
        }
        if (date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date.substring(0, 10)).atStartOfDay(NEW_YORK).toInstant();
    }

    /**
     * 计算指标变化，国债收益率使用基点，其余使用绝对变化。
     *
     * @param type 指标类型
     * @param current 当前值
     * @param previous 前值
     * @return 指标变化
     */
    private BigDecimal change(MarketMetricType type, BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return null;
        }
        BigDecimal difference = current.subtract(previous);
        if (type == MarketMetricType.US2Y || type == MarketMetricType.US10Y || type == MarketMetricType.US30Y) {
            return difference.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        return difference.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取指标单位。
     *
     * @param type 指标类型
     * @return 单位
     */
    private String unit(MarketMetricType type) {
        if (type == MarketMetricType.US2Y || type == MarketMetricType.US10Y || type == MarketMetricType.US30Y) {
            return "percent; change=bps";
        }
        if (type == MarketMetricType.WTI || type == MarketMetricType.BRENT || type == MarketMetricType.GOLD) {
            return "USD; change=absolute";
        }
        return "index; change=absolute";
    }

    /**
     * 构建供应商不支持或暂时缺失的指标。
     *
     * @param type 指标类型
     * @param reason 缺失原因
     * @return 空值指标
     */
    private MarketMetricDTO unavailable(MarketMetricType type, String reason) {
        log.warn("Market metric unavailable: metric={} reason={}", type, reason);
        return new MarketMetricDTO(type.name(), null, null, unit(type), null, SOURCE);
    }

    /**
     * 读取数值字段。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return 数值
     */
    private BigDecimal decimal(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank() || ".".equals(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    /**
     * 计算百分比变化。
     *
     * @param current 当前值
     * @param previous 前值
     * @return 百分比变化
     */
    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 4, RoundingMode.HALF_UP);
    }

    /**
     * 构建不包含密钥的供应商异常。
     *
     * @param operation 操作描述
     * @param response 供应商响应
     * @return 供应商异常
     */
    private IllegalStateException providerError(String operation, JsonNode response) {
        String message = response.path("Error Message").asText();
        if (message.isBlank()) {
            message = response.path("Information").asText();
        }
        if (message.isBlank()) {
            message = response.path("Note").asText();
        }
        if (message.isBlank()) {
            message = "missing expected data";
        }
        log.warn("Alpha Vantage response unavailable: operation={} reason={}", operation, message);
        return new IllegalStateException("Alpha Vantage could not provide " + operation + ": " + message);
    }

}

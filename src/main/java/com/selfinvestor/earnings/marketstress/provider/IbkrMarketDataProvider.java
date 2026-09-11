package com.selfinvestor.earnings.marketstress.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 IBKR Client Portal Web API 的行情供应商。
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "market-stress.provider", name = "market-data", havingValue = "ibkr")
public class IbkrMarketDataProvider implements MarketDataProvider {
    private static final String SOURCE = "IBKR Client Portal Web API";
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final String SNAPSHOT_FIELDS = "31,55,82,83,6004,6509";

    @Resource
    private IbkrWebApiClient client;
    @Resource
    private MarketStressProperties properties;

    private final Map<String, Long> conidCache = new ConcurrentHashMap<>();
    private final Map<String, HistoryCacheEntry> historyCache = new ConcurrentHashMap<>();

    /**
     * 获取标的最新实时行情。该接口使用标准 top-of-book snapshot，不调用收费监管快照接口。
     *
     * @param symbol 标的代码
     * @return 最新行情
     */
    @Override
    public MarketQuoteDTO getQuote(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        JsonNode snapshot = requestSnapshot(resolveStockConid(normalizedSymbol), normalizedSymbol);
        MarketQuoteDTO quote = new MarketQuoteDTO();
        quote.setSymbol(normalizedSymbol);
        quote.setPrice(requiredDecimal(snapshot, "31", "last price for " + normalizedSymbol));
        quote.setDailyReturn(decimal(snapshot.path("83").asText()));
        quote.setTimestamp(snapshotTimestamp(snapshot));
        quote.setSource(SOURCE);
        return quote;
    }

    /**
     * 获取 IBKR 日线并按纽约交易日期升序返回，同一纽约交易日内复用缓存。
     *
     * @param symbol 标的代码
     * @param days 最大交易日数量
     * @return 日线行情
     */
    @Override
    public List<DailyBarDTO> getDailyBars(String symbol, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be greater than zero");
        }
        String normalizedSymbol = normalizeSymbol(symbol);
        LocalDate today = LocalDate.now(NEW_YORK);
        HistoryCacheEntry cached = historyCache.get(normalizedSymbol);
        if (cached != null && cached.getCacheDate().equals(today)
                && cached.getRequestedDays() >= days) {
            return copyLast(cached.getBars(), days);
        }
        int requestedDays = Math.max(days, 220);
        List<DailyBarDTO> bars = requestDailyBars(normalizedSymbol, requestedDays);
        historyCache.put(normalizedSymbol, new HistoryCacheEntry(today, requestedDays, bars));
        return copyLast(bars, days);
    }

    /**
     * 获取 IBKR 可准确识别的跨资产指标。第一版仅直接支持 VIX，其他宏观序列返回空值。
     *
     * @param type 指标类型
     * @return 市场指标
     */
    @Override
    public MarketMetricDTO getMetric(MarketMetricType type) {
        if (type != MarketMetricType.VIX) {
            log.debug("IBKR metric has no unambiguous spot-series mapping: metric={}", type);
            return new MarketMetricDTO(type.name(), null, null, null, null, SOURCE);
        }
        try {
            long conid = resolveIndexConid("VIX");
            JsonNode snapshot = requestSnapshot(conid, "VIX");
            return new MarketMetricDTO(type.name(), requiredDecimal(snapshot, "31", "VIX"),
                    decimal(snapshot.path("82").asText()), "index points; change=points",
                    snapshotTimestamp(snapshot), SOURCE);
        } catch (RuntimeException exception) {
            log.warn("IBKR VIX unavailable: reason={}", exception.getMessage());
            return new MarketMetricDTO(type.name(), null, null, "index points; change=points", null, SOURCE);
        }
    }

    /**
     * 请求历史日线。
     *
     * @param symbol 标的代码
     * @param days 需要保留的交易日数量
     * @return 日线行情
     */
    private List<DailyBarDTO> requestDailyBars(String symbol, int days) {
        int calendarDays = Math.min(1000, Math.max(days + 30, days * 2));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("conid", resolveStockConid(symbol));
        parameters.put("period", calendarDays + "d");
        parameters.put("bar", "1d");
        parameters.put("outsideRth", false);
        parameters.put("source", "Last");
        JsonNode response = client.get("/iserver/marketdata/history", parameters);
        JsonNode data = response.path("data");
        if (!data.isArray() || data.size() == 0) {
            throw new IllegalStateException("IBKR returned no historical bars for " + symbol);
        }
        List<DailyBarDTO> bars = new ArrayList<>();
        for (JsonNode item : data) {
            Instant timestamp = Instant.ofEpochMilli(item.path("t").asLong());
            bars.add(new DailyBarDTO(symbol, timestamp.atZone(NEW_YORK).toLocalDate(),
                    nodeDecimal(item, "o"), nodeDecimal(item, "h"), nodeDecimal(item, "l"),
                    nodeDecimal(item, "c"), nodeDecimal(item, "v"), timestamp, SOURCE));
        }
        bars.sort(Comparator.comparing(DailyBarDTO::getDate));
        if (CollectionUtils.isEmpty(bars) || bars.get(bars.size() - 1).getClose() == null) {
            throw new IllegalStateException("IBKR historical bars contain no usable close for " + symbol);
        }
        return copyLast(bars, days);
    }

    /**
     * 执行 IBKR top-of-book 预热与读取请求。
     *
     * @param conid IBKR 合约编号
     * @param symbol 标的代码
     * @return 带价格字段的快照
     */
    private JsonNode requestSnapshot(long conid, String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("conids", conid);
        parameters.put("fields", SNAPSHOT_FIELDS);
        JsonNode response = client.get("/iserver/marketdata/snapshot", parameters);
        JsonNode snapshot = findSnapshot(response, conid);
        if (snapshot == null || decimal(snapshot.path("31").asText()) == null) {
            waitForPreflight();
            response = client.get("/iserver/marketdata/snapshot", parameters);
            snapshot = findSnapshot(response, conid);
        }
        if (snapshot == null) {
            throw new IllegalStateException("IBKR returned no market snapshot for " + symbol);
        }
        validateMarketDataAvailability(snapshot, symbol);
        return snapshot;
    }

    /**
     * 从行情响应中选择指定合约。
     *
     * @param response 行情响应
     * @param conid IBKR 合约编号
     * @return 行情节点，找不到时返回 null
     */
    private JsonNode findSnapshot(JsonNode response, long conid) {
        if (!response.isArray()) {
            return null;
        }
        for (JsonNode item : response) {
            if (item.path("conid").asLong(-1L) == conid) {
                return item;
            }
        }
        return null;
    }

    /**
     * 校验 IBKR 行情是否为实时数据。
     *
     * @param snapshot 行情快照
     * @param symbol 标的代码
     */
    private void validateMarketDataAvailability(JsonNode snapshot, String symbol) {
        if (!properties.getProvider().getIbkr().isRequireRealtime()) {
            return;
        }
        String availability = snapshot.path("6509").asText();
        if (availability.isBlank() || availability.charAt(0) != 'R') {
            throw new IllegalStateException("IBKR real-time market data is unavailable for " + symbol
                    + "; availability=" + (availability.isBlank() ? "UNKNOWN" : availability));
        }
    }

    /**
     * 等待 IBKR 首次请求建立行情流。
     */
    private void waitForPreflight() {
        DurationSleeper.sleep(properties.getProvider().getIbkr().getSnapshotPreflightDelay());
    }

    /**
     * 解析或查询股票、ETF 的 IBKR conid。
     *
     * @param symbol 标的代码
     * @return IBKR conid
     */
    private long resolveStockConid(String symbol) {
        return conidCache.computeIfAbsent("STK:" + symbol, key -> {
            Long configured = configuredConid(symbol);
            if (configured != null) {
                return configured;
            }
            JsonNode response = client.get("/trsrv/stocks", Map.of("symbols", symbol));
            JsonNode instruments = response.path(symbol);
            if (!instruments.isArray()) {
                instruments = response.path(symbol.toLowerCase(Locale.ROOT));
            }
            Long resolved = firstStockConid(instruments);
            if (resolved == null) {
                throw new IllegalStateException("Cannot resolve IBKR stock conid for " + symbol);
            }
            log.info("IBKR contract resolved: symbol={} conid={}", symbol, resolved);
            return resolved;
        });
    }

    /**
     * 从股票搜索响应中选择美国股票或 ETF 合约。
     *
     * @param instruments 搜索响应中的标的数组
     * @return conid，找不到时返回 null
     */
    private Long firstStockConid(JsonNode instruments) {
        if (!instruments.isArray()) {
            return null;
        }
        Long fallback = null;
        for (JsonNode instrument : instruments) {
            if (!"STK".equalsIgnoreCase(instrument.path("assetClass").asText())) {
                continue;
            }
            JsonNode contracts = instrument.path("contracts");
            if (!contracts.isArray()) {
                continue;
            }
            for (JsonNode contract : contracts) {
                long conid = contract.path("conid").asLong(-1L);
                if (conid <= 0) {
                    continue;
                }
                if (fallback == null) {
                    fallback = conid;
                }
                if (contract.path("isUS").asBoolean(false)) {
                    return conid;
                }
            }
        }
        return fallback;
    }

    /**
     * 解析或查询指数的 IBKR conid。
     *
     * @param symbol 指数代码
     * @return IBKR conid
     */
    private long resolveIndexConid(String symbol) {
        return conidCache.computeIfAbsent("IND:" + symbol, key -> {
            Long configured = configuredConid(symbol);
            if (configured != null) {
                return configured;
            }
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", symbol);
            parameters.put("secType", "IND");
            JsonNode response = client.get("/iserver/secdef/search", parameters);
            if (response.isArray()) {
                for (JsonNode contract : response) {
                    if (symbol.equalsIgnoreCase(contract.path("symbol").asText())
                            && "IND".equalsIgnoreCase(contract.path("secType").asText())) {
                        long conid = contract.path("conid").asLong(-1L);
                        if (conid > 0) {
                            log.info("IBKR index contract resolved: symbol={} conid={}", symbol, conid);
                            return conid;
                        }
                    }
                }
            }
            throw new IllegalStateException("Cannot resolve IBKR index conid for " + symbol);
        });
    }

    /**
     * 读取配置中的固定 conid 映射。
     *
     * @param symbol 标的代码
     * @return conid，未配置时返回 null
     */
    private Long configuredConid(String symbol) {
        Map<String, Long> configured = properties.getProvider().getIbkr().getConids();
        if (MapUtils.isEmpty(configured)) {
            return null;
        }
        Long exact = configured.get(symbol);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Long> entry : configured.entrySet()) {
            if (symbol.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 读取必需的小数字段。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @param description 字段说明
     * @return 数值
     */
    private BigDecimal requiredDecimal(JsonNode node, String field, String description) {
        BigDecimal value = decimal(node.path(field).asText());
        if (value == null) {
            throw new IllegalStateException("IBKR returned no " + description);
        }
        return value;
    }

    /**
     * 解析 IBKR 格式化数值，兼容前收盘价和百分号前缀。
     *
     * @param raw 原始字符串
     * @return 数值，无法解析时返回 null
     */
    private BigDecimal decimal(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw)) {
            return null;
        }
        String normalized = raw.trim().replace(",", "").replace("%", "");
        while (!normalized.isBlank() && Character.isLetter(normalized.charAt(0))) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 读取 JSON 数值字段。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return 数值
     */
    private BigDecimal nodeDecimal(JsonNode node, String field) {
        if (node.path(field).isNumber()) {
            return node.path(field).decimalValue();
        }
        return decimal(node.path(field).asText());
    }

    /**
     * 读取 IBKR 行情更新时间。
     *
     * @param snapshot 行情快照
     * @return UTC 时间戳
     */
    private Instant snapshotTimestamp(JsonNode snapshot) {
        long updated = snapshot.path("_updated").asLong(0L);
        if (updated <= 0) {
            return Instant.now();
        }
        return Instant.ofEpochMilli(updated);
    }

    /**
     * 校验并统一标的代码。
     *
     * @param symbol 原始标的代码
     * @return 大写标的代码
     */
    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 复制日线尾部，避免盘中报价合并修改缓存。
     *
     * @param bars 原始日线
     * @param days 返回数量
     * @return 日线副本
     */
    private List<DailyBarDTO> copyLast(List<DailyBarDTO> bars, int days) {
        int fromIndex = Math.max(0, bars.size() - days);
        List<DailyBarDTO> result = new ArrayList<>();
        for (int index = fromIndex; index < bars.size(); index++) {
            DailyBarDTO bar = bars.get(index);
            result.add(new DailyBarDTO(bar.getSymbol(), bar.getDate(), bar.getOpen(), bar.getHigh(), bar.getLow(),
                    bar.getClose(), bar.getVolume(), bar.getTimestamp(), bar.getSource()));
        }
        return result;
    }

    /**
     * 当日历史日线缓存。
     */
    private static final class HistoryCacheEntry {
        private final LocalDate cacheDate;
        private final int requestedDays;
        private final List<DailyBarDTO> bars;

        /**
         * 创建历史日线缓存。
         *
         * @param cacheDate 缓存日期
         * @param requestedDays 请求交易日数量
         * @param bars 日线数据
         */
        private HistoryCacheEntry(LocalDate cacheDate, int requestedDays, List<DailyBarDTO> bars) {
            this.cacheDate = cacheDate;
            this.requestedDays = requestedDays;
            this.bars = bars;
        }

        /**
         * 获取缓存日期。
         *
         * @return 缓存日期
         */
        private LocalDate getCacheDate() {
            return cacheDate;
        }

        /**
         * 获取原请求交易日数量。
         *
         * @return 请求交易日数量
         */
        private int getRequestedDays() {
            return requestedDays;
        }

        /**
         * 获取缓存日线。
         *
         * @return 日线数据
         */
        private List<DailyBarDTO> getBars() {
            return bars;
        }
    }

    /**
     * 可中断的短时间等待工具。
     */
    private static final class DurationSleeper {
        /**
         * 等待指定时长。
         *
         * @param duration 等待时长
         */
        private static void sleep(java.time.Duration duration) {
            if (duration == null || duration.isZero() || duration.isNegative()) {
                return;
            }
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for IBKR market data preflight", exception);
            }
        }
    }
}

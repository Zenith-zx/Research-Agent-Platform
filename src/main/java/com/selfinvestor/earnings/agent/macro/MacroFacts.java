package com.selfinvestor.earnings.agent.macro;

import com.selfinvestor.earnings.agent.macro.model.MacroRelease;
import com.selfinvestor.earnings.agent.runtime.tool.AgentToolContext;
import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.provider.MarketDataProvider;
import com.selfinvestor.earnings.marketstress.provider.MarketNewsProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

/**
 * 只读宏观事实服务，复用行情与新闻 Provider；数据缺失返回显式限制。
 */
@Service
public class MacroFacts {
    @Resource
    private MacroReleaseService releases;
    @Resource
    private MarketDataProvider market;
    @Resource
    private MarketNewsProvider news;

    /**
     * 按工具能力读取事实；分发与分析路径无关，调用顺序完全由模型决定。
     */
    public Object read(String tool, AgentToolContext context, Map<String, Object> args) {
        return switch (tool) {
            case "get_macro_release" -> release(context, args, null);
            case "get_macro_history" -> history(args);
            case "get_cpi_components" -> components(context, args);
            case "get_fomc_statement" -> release(context, args, "FOMC");
            case "compare_previous_fomc_statement" -> compare(context, args);
            case "get_treasury_yields" -> yields();
            case "get_dollar_index" -> metric(MarketMetricType.DXY);
            case "get_vix" -> metric(MarketMetricType.VIX);
            case "get_oil_price" -> metric(MarketMetricType.WTI);
            case "get_market_index" -> index(args);
            case "get_macro_news" -> news.getRecentMarketNews(Duration.ofHours(48), 12);
            default -> throw new IllegalArgumentException("Unknown macro tool: " + tool);
        };
    }

    /**
     * 优先按明确 releaseId 读取；否则选择指定指标/period 的最新已导入发布。
     */
    private MacroRelease release(AgentToolContext context, Map<String, Object> args, String expectedIndicator) {
        String id = text(args, "releaseId", "");
        String indicator = text(args, "indicator", expectedIndicator == null ? "" : expectedIndicator);
        String period = text(args, "period", "");
        MacroRelease result;
        if (!id.isBlank()) {
            result = releases.require(id);
        } else if (args.containsKey("indicator") || args.containsKey("period")) {
            result = latest(indicator, period);
        } else {
            Object originalId = context.getSubmission().getMetadata().get("releaseId");
            if (originalId != null) {
                MacroRelease original = releases.require(originalId.toString());
                result = expectedIndicator == null || expectedIndicator.equals(original.getIndicator())
                        ? original : latest(expectedIndicator, "");
            } else if (!indicator.isBlank()) {
                result = latest(indicator, period);
            } else {
                throw new IllegalArgumentException("Provide releaseId or indicator for user-input turns");
            }
        }
        if (expectedIndicator != null && !expectedIndicator.equals(result.getIndicator())) {
            throw new IllegalArgumentException("This tool requires a " + expectedIndicator + " release");
        }
        return result;
    }

    /**
     * 显式筛选时读取最近发布，不替换已有启动事件的默认 releaseId。
     */
    private MacroRelease latest(String indicator, String period) {
        List<MacroRelease> history = releases.history(indicator, period.isBlank() ? null : period, 1);
        if (history.isEmpty()) {
            throw new IllegalStateException("No imported release for indicator/period");
        }
        return history.get(0);
    }

    /**
     * 返回最多二十四期已导入历史；修订会保留发布版本，不暗示连续官方序列。
     */
    private Object history(Map<String, Object> args) {
        String indicator = text(args, "indicator", "CPI");
        var values = releases.history(indicator, null, Integer.parseInt(text(args, "limit", "12")));
        if (values.isEmpty()) {
            return unavailable("No imported history for " + indicator);
        }
        return Map.of("available", true, "coverage", "Imported observations only; may contain gaps and revisions",
                "releases", values);
    }

    /**
     * 只返回导入的 CPI 分项，不从 headline 推造 Shelter、Supercore 等分项。
     */
    private Object components(AgentToolContext context, Map<String, Object> args) {
        MacroRelease release = release(context, args, "CPI");
        if (release.getComponents() == null || release.getComponents().isEmpty()) {
            return unavailable("CPI components not supplied by the imported source");
        }
        return Map.of("releaseId", release.getReleaseId(), "releaseDate", release.getReleaseDate(),
                "source", release.getSource(), "components", release.getComponents());
    }

    /**
     * 返回本次和前次原文，不在 Java 内固定解释鹰鸽意义。
     */
    private Object compare(AgentToolContext context, Map<String, Object> args) {
        MacroRelease current = release(context, args, "FOMC");
        if (current.getStatement() == null || current.getPreviousStatement() == null) {
            return unavailable("Both current and previous statements must be imported for comparison");
        }
        return Map.of("releaseId", current.getReleaseId(), "source", current.getSource(),
                "releaseDate", current.getReleaseDate(), "currentStatement", current.getStatement(),
                "previousStatement", current.getPreviousStatement(),
                "comparisonMode", "Source texts side by side; interpretation belongs to the agent");
    }

    /**
     * 返回四个期限和可计算的利差；供应商无数据时保持 null，不使用替代报价。
     */
    private Object yields() {
        Map<String, MarketMetricDTO> values = new LinkedHashMap<>();
        for (MarketMetricType type : List.of(MarketMetricType.US2Y, MarketMetricType.US5Y,
                MarketMetricType.US10Y, MarketMetricType.US30Y)) {
            values.put(type.name(), market.getMetric(type));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yields", values);
        result.put("2s10s", spread(values.get("US2Y"), values.get("US10Y")));
        result.put("5s30s", spread(values.get("US5Y"), values.get("US30Y")));
        result.put("limitation", "Null values/spreads mean unavailable; existing Finnhub provider has no Treasury feed. "
                + "Spreads require same timestamp and unit; quotations are not an event-window reaction.");
        return result;
    }

    /**
     * 仅在同时间同口径时计算长端减短端，单位保持输入单位。
     */
    private Object spread(MarketMetricDTO shortEnd, MarketMetricDTO longEnd) {
        if (shortEnd == null || longEnd == null || shortEnd.getValue() == null || longEnd.getValue() == null
                || shortEnd.getTimestamp() == null || shortEnd.getUnit() == null
                || !Objects.equals(shortEnd.getTimestamp(), longEnd.getTimestamp())
                || !Objects.equals(shortEnd.getUnit(), longEnd.getUnit())) {
            return null;
        }
        return Map.of("value", longEnd.getValue().subtract(shortEnd.getValue()), "unit", shortEnd.getUnit(),
                "timestamp", shortEnd.getTimestamp());
    }

    /**
     * 包装已有跨资产行情，空值显式标记不可用。
     */
    private Object metric(MarketMetricType type) {
        MarketMetricDTO metric = market.getMetric(type);
        if (metric == null || metric.getValue() == null) {
            return unavailable("Current provider has no usable " + type + " observation");
        }
        return metric;
    }

    /**
     * 只允许 V1 的三个指数 ETF，返回最新快照而非虚构的事件前后变动。
     */
    private Object index(Map<String, Object> args) {
        String symbol = text(args, "symbol", "QQQ").toUpperCase(Locale.ROOT);
        if (!Set.of("SPY", "QQQ", "IWM").contains(symbol)) {
            throw new IllegalArgumentException("Supported market indices: SPY, QQQ, IWM");
        }
        return Map.of("quote", market.getQuote(symbol), "dailyHistory", market.getDailyBars(symbol, 20),
                "limitation", "Current quote and locally recorded closes; daily return is NOT event-specific reaction");
    }

    /**
     * 生成机器可读的数据缺失结果。
     */
    private Map<String, Object> unavailable(String reason) {
        return Map.of("available", false, "limitation", reason);
    }

    /**
     * 读取可选参数，缺失时使用声明的默认值。
     */
    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}

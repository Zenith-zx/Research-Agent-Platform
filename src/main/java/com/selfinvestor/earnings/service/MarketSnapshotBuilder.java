package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.CrossAssetSnapshotDTO;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketSnapshotDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import com.selfinvestor.earnings.marketstress.provider.MarketDataProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 市场快照构建服务。
 */
@Service
@Slf4j
public class MarketSnapshotBuilder {
    @Resource
    private MarketDataProvider marketDataProvider;
    @Resource
    private TechnicalIndicatorService technicalIndicatorService;

    /**
     * 基于关键标的历史行情构建完整市场快照。
     *
     * @param symbol 触发标的
     * @param triggerBars 触发标的日线行情
     * @return 市场快照
     */
    public MarketSnapshotDTO build(String symbol, List<DailyBarDTO> triggerBars) {
        if (CollectionUtils.isEmpty(triggerBars)) {
            throw new IllegalArgumentException("Trigger symbol history is required");
        }
        MarketSnapshotDTO snapshot = new MarketSnapshotDTO();
        snapshot.setTimestamp(Instant.now());
        snapshot.setQqq(buildTechnicalSnapshot(symbol, triggerBars));
        snapshot.setCrossAsset(buildCrossAsset());
        return snapshot;
    }

    /**
     * 构建触发标的技术指标。
     *
     * @param symbol 标的代码
     * @param bars 日线行情
     * @return 技术快照
     */
    public SymbolSnapshotDTO buildTechnicalSnapshot(String symbol, List<DailyBarDTO> bars) {
        if (CollectionUtils.isEmpty(bars)) {
            throw new IllegalArgumentException("Symbol history is required");
        }
        List<BigDecimal> closes = bars.stream().map(DailyBarDTO::getClose).collect(Collectors.toList());
        DailyBarDTO latest = bars.get(bars.size() - 1);
        SymbolSnapshotDTO result = new SymbolSnapshotDTO();
        result.setSymbol(symbol.toUpperCase());
        result.setPrice(latest.getClose());
        result.setDailyReturn(periodReturn(closes, 1));
        result.setFiveDayReturn(periodReturn(closes, 5));
        result.setTwentyDayReturn(periodReturn(closes, 20));
        result.setRsi6(technicalIndicatorService.calculateRsi(closes, 6).orElse(null));
        result.setRsi14(technicalIndicatorService.calculateRsi(closes, 14).orElse(null));
        result.setDistanceFromMa20(distanceFromMovingAverage(closes, 20));
        result.setDistanceFromMa50(distanceFromMovingAverage(closes, 50));
        result.setDistanceFromMa200(distanceFromMovingAverage(closes, 200));
        result.setTimestamp(latest.getTimestamp());
        result.setSource(latest.getSource());
        return result;
    }

    /**
     * 构建跨资产指标并允许单项数据失败。
     *
     * @return 跨资产快照
     */
    private CrossAssetSnapshotDTO buildCrossAsset() {
        CrossAssetSnapshotDTO crossAsset = new CrossAssetSnapshotDTO();
        crossAsset.setSpyReturn(safeEtfReturn("SPY"));
        crossAsset.setIwmReturn(safeEtfReturn("IWM"));
        crossAsset.setSoxxReturn(safeEtfReturn("SOXX"));
        crossAsset.setHygReturn(safeEtfReturn("HYG"));
        crossAsset.setLqdReturn(safeEtfReturn("LQD"));
        crossAsset.setVix(safeMetric(MarketMetricType.VIX));
        crossAsset.setUs2y(safeMetric(MarketMetricType.US2Y));
        crossAsset.setUs10y(safeMetric(MarketMetricType.US10Y));
        crossAsset.setUs30y(safeMetric(MarketMetricType.US30Y));
        crossAsset.setDxy(safeMetric(MarketMetricType.DXY));
        crossAsset.setWti(safeMetric(MarketMetricType.WTI));
        crossAsset.setBrent(safeMetric(MarketMetricType.BRENT));
        crossAsset.setGold(safeMetric(MarketMetricType.GOLD));
        return crossAsset;
    }

    /**
     * 安全获取 ETF 单日收益率。
     *
     * @param symbol ETF 代码
     * @return 收益率指标
     */
    private MarketMetricDTO safeEtfReturn(String symbol) {
        try {
            MarketQuoteDTO quote = marketDataProvider.getQuote(symbol);
            if (quote != null && quote.getDailyReturn() != null) {
                return new MarketMetricDTO(symbol + "_RETURN", quote.getDailyReturn(), null, "%",
                        quote.getTimestamp(), quote.getSource());
            }
            List<DailyBarDTO> bars = marketDataProvider.getDailyBars(symbol, 2);
            if (bars.size() < 2) {
                return unavailable(symbol + "_RETURN", "%");
            }
            DailyBarDTO latest = bars.get(bars.size() - 1);
            BigDecimal result = percentChange(latest.getClose(), bars.get(bars.size() - 2).getClose());
            return new MarketMetricDTO(symbol + "_RETURN", result, null, "%", latest.getTimestamp(), latest.getSource());
        } catch (RuntimeException exception) {
            log.warn("Cross-asset ETF unavailable: symbol={} reason={}", symbol, exception.getMessage());
            return unavailable(symbol + "_RETURN", "%");
        }
    }

    /**
     * 安全获取宏观市场指标。
     *
     * @param type 指标类型
     * @return 指标或空值指标
     */
    private MarketMetricDTO safeMetric(MarketMetricType type) {
        try {
            MarketMetricDTO metric = marketDataProvider.getMetric(type);
            return metric == null ? unavailable(type.name(), null) : metric;
        } catch (RuntimeException exception) {
            log.warn("Cross-asset metric unavailable: metric={} reason={}", type, exception.getMessage());
            return unavailable(type.name(), null);
        }
    }

    /**
     * 构建空值指标。
     *
     * @param name 指标名称
     * @param unit 单位
     * @return 空值指标
     */
    private MarketMetricDTO unavailable(String name, String unit) {
        return new MarketMetricDTO(name, null, null, unit, null, null);
    }

    /**
     * 计算指定交易日跨度的收益率。
     *
     * @param closes 收盘价
     * @param periods 跨度
     * @return 百分比收益率
     */
    private BigDecimal periodReturn(List<BigDecimal> closes, int periods) {
        if (closes.size() <= periods) {
            return null;
        }
        return percentChange(closes.get(closes.size() - 1), closes.get(closes.size() - 1 - periods));
    }

    /**
     * 计算当前价格距离移动平均线的百分比。
     *
     * @param closes 收盘价
     * @param period 均线周期
     * @return 距离百分比
     */
    private BigDecimal distanceFromMovingAverage(List<BigDecimal> closes, int period) {
        if (closes.size() < period) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int index = closes.size() - period; index < closes.size(); index++) {
            if (closes.get(index) == null) {
                return null;
            }
            sum = sum.add(closes.get(index));
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        return percentChange(closes.get(closes.size() - 1), average);
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
}

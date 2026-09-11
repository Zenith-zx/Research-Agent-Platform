package com.selfinvestor.earnings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.event.MarketContextReadyEvent;
import com.selfinvestor.earnings.event.MarketStressAnalysisCompletedEvent;
import com.selfinvestor.earnings.event.OversoldDetectedEvent;
import com.selfinvestor.earnings.marketstress.MarketStressAnalysisPO;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketNewsDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketSnapshotDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import com.selfinvestor.earnings.marketstress.provider.MarketDataProvider;
import com.selfinvestor.earnings.marketstress.provider.MarketNewsProvider;
import com.selfinvestor.earnings.repository.MarketStressAnalysisRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 市场压力检测、上下文和持久化编排服务。
 */
@Service
@Slf4j
public class MarketStressService {

    @Resource
    private MarketDataProvider marketDataProvider;

    @Resource
    private MarketNewsProvider marketNewsProvider;

    @Resource
    private MarketSnapshotBuilder snapshotBuilder;

    @Resource
    private OversoldTriggerService triggerService;

    @Resource
    private MarketStressCooldownService cooldownService;

    @Resource
    private MarketStressAiService aiService;

    @Resource
    private TechnicalIndicatorService technicalIndicatorService;

    @Resource
    private DailyMarketCloseService dailyMarketCloseService;

    @Resource
    private MarketStressAnalysisRepository repository;

    @Resource
    private MarketStressProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ApplicationEventPublisher events;

    @Resource
    private PlatformTransactionManager transactionManager;


    private TransactionTemplate transactions;

    /**
     * 初始化短生命周期事务模板。
     */
    @PostConstruct
    public void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 扫描标的 RSI，并在满足阈值和冷却条件时创建 append-only 任务。
     *
     * @param symbol 标的
     */
    public void scan(String symbol) {
        log.info("Market scan started: {}", symbol);
        MarketQuoteDTO quote = marketDataProvider.getQuote(symbol);
        dailyMarketCloseService.recordPreviousClose(symbol, quote);
        List<DailyBarDTO> bars = marketDataProvider.getDailyBars(symbol, 220);
        mergeLatestQuote(symbol, bars, quote);
        SymbolSnapshotDTO technical = snapshotBuilder.buildTechnicalSnapshot(symbol, bars);
        Optional<BigDecimal> triggerRsi = technicalIndicatorService.calculateRsi(
                bars.stream().map(DailyBarDTO::getClose).collect(Collectors.toList()),
                properties.getRsiPeriod());
        log.info("{} RSI{}={}", symbol, properties.getRsiPeriod(), triggerRsi.orElse(null));
        if (triggerRsi.isEmpty()) {
            log.info("Market scan is accumulating daily closes: symbol={} observations={} required={}",
                    symbol, bars.size(), properties.getRsiPeriod() + 1);
            return;
        }
        if (!triggerService.shouldTrigger(triggerRsi)) {
            return;
        }
        Instant now = Instant.now();
        if (cooldownService.isCoolingDown(symbol, now)) {
            log.info("Market stress analysis skipped by cooldown: symbol={}", symbol);
            return;
        }
        MarketStressAnalysisPO analysis = new MarketStressAnalysisPO(symbol.toUpperCase(), now, technical);
        transactions.executeWithoutResult(status -> {
            repository.insert(analysis);
            events.publishEvent(new OversoldDetectedEvent(analysis.getId(), symbol.toUpperCase(), bars));
        });
        log.info("Oversold detected: symbol={} rsiPeriod={} rsi={}", symbol, properties.getRsiPeriod(), triggerRsi.orElse(null));
    }

    /**
     * 将盘中最新报价合并到已保存的正式收盘价序列。
     *
     * @param symbol 标的
     * @param bars 日线序列
     * @param quote 最新报价
     */
    private void mergeLatestQuote(String symbol, List<DailyBarDTO> bars, MarketQuoteDTO quote) {
        if (quote == null || quote.getPrice() == null) {
            return;
        }
        LocalDate quoteDate = quote.getTimestamp().atZone(ZoneId.of("America/New_York")).toLocalDate();
        if (CollectionUtils.isEmpty(bars)) {
            bars.add(new DailyBarDTO(symbol, quoteDate, quote.getPrice(), quote.getPrice(), quote.getPrice(),
                    quote.getPrice(), null, quote.getTimestamp(), quote.getSource()));
            return;
        }
        DailyBarDTO latest = bars.get(bars.size() - 1);
        if (latest.getDate().equals(quoteDate)) {
            latest.setClose(quote.getPrice());
            latest.setTimestamp(quote.getTimestamp());
            latest.setSource(quote.getSource());
        } else if (quoteDate.isAfter(latest.getDate())) {
            bars.add(new DailyBarDTO(symbol, quoteDate, quote.getPrice(), quote.getPrice(), quote.getPrice(),
                    quote.getPrice(), null, quote.getTimestamp(), quote.getSource()));
        }
    }

    /**
     * 构建跨资产与新闻上下文并发布准备完成事件。
     *
     * @param event 超卖检测事件
     */
    public void prepareContext(OversoldDetectedEvent event) {
        MarketSnapshotDTO snapshot = snapshotBuilder.build(event.getSymbol(), event.getDailyBars());
        List<MarketNewsDTO> news;
        try {
            news = marketNewsProvider.getRecentMarketNews(
                    Duration.ofHours(properties.getProvider().getNewsLookbackHours()),
                    properties.getProvider().getMaxNewsArticles());
        } catch (RuntimeException exception) {
            log.warn("Market news unavailable, continuing without news: reason={}", exception.getMessage());
            news = new ArrayList<>();
        }
        MarketStressAnalysisPO po = repository.findById(event.getAnalysisId()).orElseThrow();
        copySnapshotFields(po, snapshot);
        po.setMarketSnapshotJson(writeJson(snapshot));
        po.setNewsJson(writeJson(news));
        transactions.executeWithoutResult(status -> {
            repository.updateContext(po);
            events.publishEvent(new MarketContextReadyEvent(po.getId()));
        });
        log.info("Market snapshot ready: symbol={}", po.getSymbol());
        log.info("News context ready, articles={}", news.size());
    }

    /**
     * 加载已持久化上下文、执行 AI 分析并保存最终结果。
     *
     * @param analysisId 分析主键
     */
    public void analyze(Long analysisId) {
        MarketStressAnalysisPO po = repository.findById(analysisId).orElseThrow();
        transactions.executeWithoutResult(status -> repository.markAnalyzing(analysisId));
        MarketSnapshotDTO snapshot = readJson(po.getMarketSnapshotJson(), MarketSnapshotDTO.class);
        List<MarketNewsDTO> news = readJson(po.getNewsJson(), new TypeReference<List<MarketNewsDTO>>() { });
        MarketStressAnalysisDTO analysis = aiService.analyze(snapshot, news);
        po.complete(analysis);
        po.setAnalysisJson(writeJson(analysis));
        transactions.executeWithoutResult(status -> {
            repository.updateCompleted(po);
            events.publishEvent(new MarketStressAnalysisCompletedEvent(po.getId()));
        });
    }

    /**
     * 将市场压力任务标记为失败。
     *
     * @param analysisId 分析主键
     * @param exception 失败异常
     */
    public void fail(Long analysisId, Exception exception) {
        String message = exception.getMessage() == null ? "Unknown error" : exception.getMessage();
        transactions.executeWithoutResult(status -> repository.updateFailed(analysisId,
                message.substring(0, Math.min(2000, message.length()))));
        log.error("Market stress processing failed: analysisId={} error={}", analysisId, exception.getMessage(), exception);
    }

    /**
     * 将快照中的常用查询值复制到持久化对象。
     *
     * @param po 持久化对象
     * @param snapshot 市场快照
     */
    private void copySnapshotFields(MarketStressAnalysisPO po, MarketSnapshotDTO snapshot) {
        po.setRsi6(snapshot.getQqq().getRsi6());
        po.setRsi14(snapshot.getQqq().getRsi14());
        po.setDailyReturn(snapshot.getQqq().getDailyReturn());
        po.setFiveDayReturn(snapshot.getQqq().getFiveDayReturn());
        po.setVix(value(snapshot.getCrossAsset().getVix()));
        po.setUs10y(value(snapshot.getCrossAsset().getUs10y()));
        po.setBrent(value(snapshot.getCrossAsset().getBrent()));
        po.setHygReturn(value(snapshot.getCrossAsset().getHygReturn()));
    }

    /**
     * 安全读取市场指标值。
     *
     * @param metric 市场指标
     * @return 指标值
     */
    private java.math.BigDecimal value(MarketMetricDTO metric) {
        return metric == null ? null : metric.getValue();
    }

    /**
     * 将对象序列化为 JSON。
     *
     * @param value 对象
     * @return JSON
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize market stress data", exception);
        }
    }

    /**
     * 将 JSON 反序列化为对象。
     *
     * @param json JSON
     * @param type 目标类型
     * @param <T> 目标泛型
     * @return 对象
     */
    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize market stress data", exception);
        }
    }

    /**
     * 将 JSON 数组反序列化为泛型对象。
     *
     * @param json JSON
     * @param type 目标类型引用
     * @param <T> 目标泛型
     * @return 对象
     */
    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize market stress data", exception);
        }
    }
}

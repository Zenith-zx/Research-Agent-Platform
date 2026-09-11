package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.marketstress.DailyMarketClosePO;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.repository.DailyMarketCloseRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 每日正式收盘价采集与查询服务。
 */
@Service
@Slf4j
public class DailyMarketCloseService {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime REGULAR_CLOSE = LocalTime.of(16, 0);

    @Resource
    private DailyMarketCloseRepository repository;
    @Resource
    private UsMarketSessionService marketSessionService;

    /**
     * 使用 Finnhub Quote 的上一收盘价保存最近已结束交易日数据。
     *
     * @param symbol 标的代码
     * @param quote 最新报价
     * @return 是否新增记录
     */
    public boolean recordPreviousClose(String symbol, MarketQuoteDTO quote) {
        if (quote == null || quote.getPreviousClose() == null) {
            return false;
        }
        Instant quoteTime = quote.getTimestamp() == null ? Instant.now() : quote.getTimestamp();
        LocalDate quoteDate = quoteTime.atZone(NEW_YORK).toLocalDate();
        LocalDate tradeDate = marketSessionService.previousTradingDay(quoteDate);
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        if (repository.findBySymbolAndTradeDate(normalizedSymbol, tradeDate).isPresent()) {
            return false;
        }
        DailyMarketClosePO close = new DailyMarketClosePO();
        close.setSymbol(normalizedSymbol);
        close.setTradeDate(tradeDate);
        close.setClosePrice(quote.getPreviousClose());
        close.setSource(quote.getSource());
        try {
            repository.insert(close);
            log.info("Daily market close recorded: symbol={} tradeDate={} close={} source={}",
                    normalizedSymbol, tradeDate, quote.getPreviousClose(), quote.getSource());
            return true;
        } catch (DuplicateKeyException exception) {
            log.debug("Daily market close already recorded concurrently: symbol={} tradeDate={}",
                    normalizedSymbol, tradeDate);
            return false;
        }
    }

    /**
     * 查询最近每日收盘价并转换为通用日线 DTO。
     *
     * @param symbol 标的代码
     * @param limit 最大数量
     * @return 日线 DTO
     */
    public List<DailyBarDTO> findLatestBars(String symbol, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        List<DailyMarketClosePO> closes = repository.findLatest(symbol, limit);
        List<DailyBarDTO> bars = new ArrayList<>();
        if (CollectionUtils.isEmpty(closes)) {
            return bars;
        }
        for (DailyMarketClosePO close : closes) {
            Instant timestamp = close.getTradeDate().atTime(REGULAR_CLOSE).atZone(NEW_YORK).toInstant();
            bars.add(new DailyBarDTO(close.getSymbol(), close.getTradeDate(), close.getClosePrice(),
                    close.getClosePrice(), close.getClosePrice(), close.getClosePrice(), null,
                    timestamp, close.getSource()));
        }
        return bars;
    }
}

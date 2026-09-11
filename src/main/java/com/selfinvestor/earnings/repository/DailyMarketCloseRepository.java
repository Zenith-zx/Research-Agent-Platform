package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.marketstress.DailyMarketClosePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日收盘价 MyBatis Repository。
 */
@Mapper
public interface DailyMarketCloseRepository {
    /**
     * 插入每日收盘价。
     *
     * @param close 每日收盘价
     */
    void insert(DailyMarketClosePO close);

    /**
     * 按标的和交易日期查询收盘价。
     *
     * @param symbol 标的代码
     * @param tradeDate 交易日期
     * @return 收盘价记录
     */
    Optional<DailyMarketClosePO> findBySymbolAndTradeDate(@Param("symbol") String symbol,
                                                          @Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询标的最近若干个交易日的收盘价并按日期升序返回。
     *
     * @param symbol 标的代码
     * @param limit 最大数量
     * @return 收盘价记录
     */
    List<DailyMarketClosePO> findLatest(@Param("symbol") String symbol, @Param("limit") int limit);
}

package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.enums.MarketMetricType;
import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketMetricDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketQuoteDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketSnapshotDTO;
import com.selfinvestor.earnings.marketstress.provider.MarketDataProvider;
import com.selfinvestor.earnings.service.MarketSnapshotBuilder;
import com.selfinvestor.earnings.service.TechnicalIndicatorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSnapshotBuilderTest {
    /**
     * 验证跨资产数据缺失时仍能生成 QQQ 快照。
     */
    @Test
    void toleratesMissingCrossAssetData() {
        MarketSnapshotBuilder builder = new MarketSnapshotBuilder();
        ReflectionTestUtils.setField(builder, "marketDataProvider", new FailingCrossAssetProvider());
        ReflectionTestUtils.setField(builder, "technicalIndicatorService", new TechnicalIndicatorService());
        MarketSnapshotDTO snapshot = builder.build("QQQ", bars(20));
        assertThat(snapshot.getQqq().getRsi6()).isNotNull();
        assertThat(snapshot.getCrossAsset().getVix().getValue()).isNull();
        assertThat(snapshot.getCrossAsset().getSpyReturn().getValue()).isNull();
    }

    /**
     * 创建测试日线数据。
     *
     * @param count 数量
     * @return 日线数据
     */
    private List<DailyBarDTO> bars(int count) {
        List<DailyBarDTO> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BigDecimal close = BigDecimal.valueOf(120 - index);
            result.add(new DailyBarDTO("QQQ", LocalDate.of(2026, 1, 1).plusDays(index), close, close, close,
                    close, BigDecimal.TEN, Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index * 86400L), "test"));
        }
        return result;
    }

    /**
     * 始终模拟跨资产失败的数据供应商。
     */
    private static class FailingCrossAssetProvider implements MarketDataProvider {
        /**
         * 获取最新行情。
         *
         * @param symbol 标的代码
         * @return 不返回
         */
        @Override
        public MarketQuoteDTO getQuote(String symbol) {
            throw new IllegalStateException("missing");
        }

        /**
         * 获取日线行情。
         *
         * @param symbol 标的代码
         * @param days 数量
         * @return 不返回
         */
        @Override
        public List<DailyBarDTO> getDailyBars(String symbol, int days) {
            throw new IllegalStateException("missing");
        }

        /**
         * 获取宏观指标。
         *
         * @param type 指标类型
         * @return 不返回
         */
        @Override
        public MarketMetricDTO getMetric(MarketMetricType type) {
            throw new IllegalStateException("missing");
        }
    }
}

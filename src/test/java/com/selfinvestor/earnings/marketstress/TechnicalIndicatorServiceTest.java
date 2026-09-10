package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.service.TechnicalIndicatorService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorServiceTest {
    /**
     * 使用 Wilder 经典价格序列验证 RSI14。
     */
    @Test
    void calculatesWilderRsi14() {
        List<BigDecimal> closes = Arrays.asList("44.34", "44.09", "44.15", "43.61", "44.33", "44.83", "45.10",
                "45.42", "45.84", "46.08", "45.89", "46.03", "45.61", "46.28", "46.28").stream()
                .map(BigDecimal::new).collect(Collectors.toList());
        BigDecimal result = new TechnicalIndicatorService().calculateRsi(closes, 14).orElseThrow();
        assertThat(result).isBetween(new BigDecimal("70.45"), new BigDecimal("70.48"));
    }

    /**
     * 验证数据不足时不返回虚假 RSI。
     */
    @Test
    void returnsEmptyWhenPricesAreInsufficient() {
        assertThat(new TechnicalIndicatorService().calculateRsi(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN), 6)).isEmpty();
    }

    /**
     * 使用可手工验证的涨跌序列验证 RSI6。
     */
    @Test
    void calculatesWilderRsi6() {
        List<BigDecimal> closes = Arrays.asList("10", "9", "8", "9", "8", "7", "8").stream()
                .map(BigDecimal::new).collect(Collectors.toList());
        BigDecimal result = new TechnicalIndicatorService().calculateRsi(closes, 6).orElseThrow();
        assertThat(result).isEqualByComparingTo("33.3333");
    }
}

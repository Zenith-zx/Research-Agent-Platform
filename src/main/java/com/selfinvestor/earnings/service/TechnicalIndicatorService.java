package com.selfinvestor.earnings.service;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Java 本地技术指标计算服务。
 */
@Service
public class TechnicalIndicatorService {
    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    /**
     * 使用 Wilder 平滑算法计算 RSI。
     *
     * @param closes 按时间升序排列的收盘价
     * @param period RSI 周期
     * @return 数据充足时返回 RSI，否则返回空
     */
    public Optional<BigDecimal> calculateRsi(List<BigDecimal> closes, int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("RSI period must be greater than zero");
        }
        if (CollectionUtils.isEmpty(closes) || closes.size() <= period) {
            return Optional.empty();
        }
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int index = 1; index <= period; index++) {
            BigDecimal change = required(closes.get(index)).subtract(required(closes.get(index - 1)), MATH_CONTEXT);
            if (change.signum() >= 0) {
                gains = gains.add(change, MATH_CONTEXT);
            } else {
                losses = losses.add(change.abs(), MATH_CONTEXT);
            }
        }
        BigDecimal divisor = BigDecimal.valueOf(period);
        BigDecimal averageGain = gains.divide(divisor, MATH_CONTEXT);
        BigDecimal averageLoss = losses.divide(divisor, MATH_CONTEXT);
        for (int index = period + 1; index < closes.size(); index++) {
            BigDecimal change = required(closes.get(index)).subtract(required(closes.get(index - 1)), MATH_CONTEXT);
            BigDecimal gain = change.signum() > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.signum() < 0 ? change.abs() : BigDecimal.ZERO;
            averageGain = averageGain.multiply(BigDecimal.valueOf(period - 1L), MATH_CONTEXT)
                    .add(gain, MATH_CONTEXT).divide(divisor, MATH_CONTEXT);
            averageLoss = averageLoss.multiply(BigDecimal.valueOf(period - 1L), MATH_CONTEXT)
                    .add(loss, MATH_CONTEXT).divide(divisor, MATH_CONTEXT);
        }
        if (averageLoss.signum() == 0) {
            return Optional.of(new BigDecimal("100.0000"));
        }
        if (averageGain.signum() == 0) {
            return Optional.of(new BigDecimal("0.0000"));
        }
        BigDecimal relativeStrength = averageGain.divide(averageLoss, MATH_CONTEXT);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100)
                .divide(BigDecimal.ONE.add(relativeStrength, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT);
        return Optional.of(rsi.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 验证收盘价不能为空。
     *
     * @param value 收盘价
     * @return 非空收盘价
     */
    private BigDecimal required(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Close price must not be null");
        }
        return value;
    }
}

package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.marketstress.MarketStressRetriggerPolicy;
import com.selfinvestor.earnings.repository.MarketStressAnalysisRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 市场压力分析冷却控制服务。
 */
@Service
public class MarketStressCooldownService {
    @Resource
    private MarketStressAnalysisRepository repository;
    @Resource
    private MarketStressProperties properties;
    @Resource
    private MarketStressRetriggerPolicy retriggerPolicy;

    /**
     * 判断标的是否仍在冷却期内。
     *
     * @param symbol 标的
     * @param now 当前 UTC 时间
     * @return 是否跳过分析
     */
    public boolean isCoolingDown(String symbol, Instant now) {
        Optional<Instant> latest = repository.findLatestActiveAnalysisTime(symbol);
        if (latest.isEmpty()) {
            return false;
        }
        boolean coolingDown = latest.get().plus(Duration.ofHours(properties.getAnalysisCooldownHours())).isAfter(now);
        return coolingDown && !retriggerPolicy.shouldRetrigger(symbol);
    }
}

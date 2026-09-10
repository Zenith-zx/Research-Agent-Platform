package com.selfinvestor.earnings.marketstress;

import com.selfinvestor.earnings.marketstress.dto.SymbolSnapshotDTO;
import com.selfinvestor.earnings.repository.MarketStressAnalysisRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MarketStressRepositoryTest {
    @Resource
    private MarketStressAnalysisRepository repository;

    /**
     * 验证市场压力记录插入、枚举映射和 cooldown 时间查询。
     */
    @Test
    void persistsAppendOnlyDetectionAndFindsLatestTime() {
        SymbolSnapshotDTO snapshot = new SymbolSnapshotDTO();
        snapshot.setRsi6(new BigDecimal("27.2000"));
        snapshot.setRsi14(new BigDecimal("35.1000"));
        Instant analysisTime = Instant.parse("2026-01-06T15:00:00Z");
        MarketStressAnalysisPO po = new MarketStressAnalysisPO("QQQ_TEST", analysisTime, snapshot);
        repository.insert(po);
        MarketStressAnalysisPO loaded = repository.findById(po.getId()).orElseThrow();
        assertThat(loaded.getRsi6()).isEqualByComparingTo("27.2000");
        assertThat(loaded.getStatus()).isEqualTo(com.selfinvestor.earnings.enums.MarketStressStatus.DETECTED);
        assertThat(repository.findLatestActiveAnalysisTime("QQQ_TEST")).contains(analysisTime);
    }
}

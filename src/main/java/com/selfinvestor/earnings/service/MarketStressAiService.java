package com.selfinvestor.earnings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfinvestor.earnings.analysis.OpenAiClient;
import com.selfinvestor.earnings.marketstress.dto.MarketNewsDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketSnapshotDTO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 市场压力 AI 分析编排服务。
 */
@Service
@Slf4j
public class MarketStressAiService {
    @Resource
    private OpenAiClient openAiClient;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private MeanReversionScoreService scoreService;

    /**
     * 将快照与有限新闻发送给模型，并由 Java 计算最终评分。
     *
     * @param snapshot 市场快照
     * @param news 最近的重要新闻
     * @return 结构化市场压力分析
     */
    public MarketStressAnalysisDTO analyze(MarketSnapshotDTO snapshot, List<MarketNewsDTO> news) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("marketSnapshot", snapshot);
        context.put("recentNews", news);
        String input;
        try {
            input = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize market stress context", exception);
        }
        log.info("AI market stress analysis started: symbol={}", snapshot.getQqq().getSymbol());
        Instant started = Instant.now();
        MarketStressAnalysisDTO analysis = openAiClient.analyzeMarketStress(input);
        analysis.setSymbol(snapshot.getQqq().getSymbol());
        analysis.setAnalysisTimestamp(snapshot.getTimestamp());
        scoreService.apply(snapshot.getQqq(), analysis);
        log.info("Market stress analysis completed, duration={}ms score={} action={}",
                Duration.between(started, Instant.now()).toMillis(), analysis.getMeanReversionScore(), analysis.getAction());
        return analysis;
    }
}

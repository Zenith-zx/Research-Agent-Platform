package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.marketstress.MarketStressAnalysisPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * 市场压力分析 MyBatis Repository。
 */
@Mapper
public interface MarketStressAnalysisRepository {
    /**
     * 插入 append-only 分析记录。
     *
     * @param analysis 分析记录
     */
    void insert(MarketStressAnalysisPO analysis);

    /**
     * 按主键查询分析记录。
     *
     * @param id 主键
     * @return 分析记录
     */
    Optional<MarketStressAnalysisPO> findById(Long id);

    /**
     * 查询标的最近一次非失败分析时间。
     *
     * @param symbol 标的
     * @return 最近分析时间
     */
    Optional<Instant> findLatestActiveAnalysisTime(String symbol);

    /**
     * 保存市场上下文并更新状态。
     *
     * @param analysis 分析记录
     */
    void updateContext(MarketStressAnalysisPO analysis);

    /**
     * 将任务标记为分析中。
     *
     * @param id 主键
     */
    void markAnalyzing(Long id);

    /**
     * 保存完成的结构化分析。
     *
     * @param analysis 分析记录
     */
    void updateCompleted(MarketStressAnalysisPO analysis);

    /**
     * 将任务标记为失败。
     *
     * @param id 主键
     * @param errorMessage 错误信息
     */
    void updateFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);
}

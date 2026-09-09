package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.analysis.AiAnalysisPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface AiAnalysisRepository {
    /**
     * 按 Filing 主键查询分析结果。
     *
     * @param filingId Filing 主键
     * @return 分析结果
     */
    Optional<AiAnalysisPO> findByFilingId(Long filingId);
    /**
     * 按主键查询分析结果。
     *
     * @param id 分析结果主键
     * @return 分析结果
     */
    Optional<AiAnalysisPO> findOneById(Long id);
    /**
     * 保存分析结果。
     *
     * @param analysis 分析实体
     * @return 影响行数
     */
    int insert(AiAnalysisPO analysis);
}

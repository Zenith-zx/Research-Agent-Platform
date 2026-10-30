package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.enums.FilingStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Mapper
public interface FilingRepository {
    /**
     * 判断 accession number 是否存在。
     *
     * @param accessionNumber SEC accession number
     * @return 是否存在
     */
    boolean existsByAccessionNumber(String accessionNumber);
    /**
     * 按 accession number 查询 Filing。
     *
     * @param accessionNumber SEC accession number
     * @return Filing
     */
    Optional<FilingPO> findByAccessionNumber(String accessionNumber);
    /**
     * 按主键查询完整 Filing。
     *
     * @param id Filing 主键
     * @return Filing
     */
    Optional<FilingPO> findOneById(Long id);
    /**
     * 按状态集合查询 Filing。
     *
     * @param statuses 状态集合
     * @return Filing 列表
     */
    List<FilingPO> findAllByStatusIn(@Param("statuses") Collection<FilingStatus> statuses);
    /**
     * 查询近期可以安全恢复的未完成 Filing。
     *
     * @param cutoffDate 最早申报日期
     * @param limit 最大返回数量
     * @return 待恢复 Filing 列表
     */
    List<FilingPO> findRecoverable(@Param("cutoffDate") LocalDate cutoffDate, @Param("limit") int limit);
    /**
     * 按主键倒序查询 Filing。
     *
     * @return Filing 列表
     */
    List<FilingPO> findAllByOrderByIdDesc();
    /**
     * 按股票代码查询最近的 Filing。
     *
     * @param ticker 股票代码
     * @param limit 最大返回数量
     * @return 最近 Filing 列表
     */
    List<FilingPO> findRecentByTicker(@Param("ticker") String ticker, @Param("limit") int limit);
    /**
     * 按主键查询 Filing。
     *
     * @param id Filing 主键
     * @return Filing
     */
    Optional<FilingPO> findById(Long id);
    /**
     * 新增 Filing。
     *
     * @param filing Filing 实体
     * @return 影响行数
     */
    int insert(FilingPO filing);
    /**
     * 修改 Filing 状态。
     *
     * @param id Filing 主键
     * @param status 处理状态
     * @param errorMessage 失败原因
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") FilingStatus status, @Param("errorMessage") String errorMessage);
    /**
     * 统计 Filing 数量。
     *
     * @return Filing 数量
     */
    long count();
}

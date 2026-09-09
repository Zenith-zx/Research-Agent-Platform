package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.company.CompanyPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CompanyRepository {
    /**
     * 查询启用监听的公司。
     *
     * @return 公司列表
     */
    List<CompanyPO> findAllByEnabledTrueOrderByTicker();
    /**
     * 按股票代码查询公司。
     *
     * @param ticker 股票代码
     * @return 公司
     */
    Optional<CompanyPO> findByTickerIgnoreCase(String ticker);
    /**
     * 按主键查询公司。
     *
     * @param id 公司主键
     * @return 公司
     */
    Optional<CompanyPO> findById(Long id);
    /**
     * 查询全部公司。
     *
     * @return 公司列表
     */
    List<CompanyPO> findAll();
    /**
     * 新增公司。
     *
     * @param company 公司实体
     * @return 影响行数
     */
    int insert(CompanyPO company);
    /**
     * 修改公司监听状态。
     *
     * @param id 公司主键
     * @param enabled 是否启用
     * @return 影响行数
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}

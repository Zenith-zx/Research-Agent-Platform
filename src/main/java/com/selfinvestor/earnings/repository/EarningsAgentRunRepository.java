package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.agent.earnings.EarningsAgentRunPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface EarningsAgentRunRepository {
    /**
     * 新增一次 Earnings Agent 运行记录及初始 checkpoint。
     *
     * @param run Agent 运行记录
     * @return 影响行数
     */
    int insert(EarningsAgentRunPO run);

    /**
     * 更新 Agent 运行状态和最新 checkpoint。
     *
     * @param run Agent 运行记录
     * @return 影响行数
     */
    int update(EarningsAgentRunPO run);

    /**
     * 按运行标识查询 Agent 运行记录。
     *
     * @param runId 运行标识
     * @return Agent 运行记录
     */
    Optional<EarningsAgentRunPO> findByRunId(String runId);
}

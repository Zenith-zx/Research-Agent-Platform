package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.agent.runtime.turn.AgentTurn;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * Agent Turn 的 MyBatis 持久化接口。
 */
@Mapper
public interface AgentTurnRepository {
    /**
     * 按时间倒序查询 Thread 最近二十次 Turn。
     */
    java.util.List<AgentTurn> findRecentByThreadId(String threadId);
    /**
     * 新增 Turn。
     *
     * @param turn Turn 实体
     * @return 影响行数
     */
    int insert(AgentTurn turn);

    /**
     * 更新 Turn 的状态、计数和最终结果。
     *
     * @param turn Turn 实体
     * @return 影响行数
     */
    int update(AgentTurn turn);

    /**
     * 按主键查询 Turn。
     *
     * @param id Turn 标识
     * @return Turn
     */
    Optional<AgentTurn> findById(String id);

    /**
     * 查询 Thread 当前尚未结束的 Turn。
     *
     * @param threadId Thread 标识
     * @return 活跃 Turn
     */
    Optional<AgentTurn> findActiveByThreadId(String threadId);
}

package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.agent.runtime.turn.AgentTurnItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent Turn Item 的 MyBatis 持久化接口。
 */
@Mapper
public interface AgentTurnItemRepository {
    /**
     * 新增 Turn Item。
     *
     * @param item Turn Item
     * @return 影响行数
     */
    int insert(AgentTurnItem item);

    /**
     * 按执行顺序读取 Turn 的全部 Item。
     *
     * @param turnId Turn 标识
     * @return Item 列表
     */
    List<AgentTurnItem> findByTurnId(String turnId);

    /**
     * 查询 Turn 当前最大的事件序号。
     *
     * @param turnId Turn 标识
     * @return 最大序号；没有记录时为零
     */
    int findMaxSequence(String turnId);
}

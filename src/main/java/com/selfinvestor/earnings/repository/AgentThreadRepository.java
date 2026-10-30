package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * Agent Thread 的 MyBatis 持久化接口。
 */
@Mapper
public interface AgentThreadRepository {
    /**
     * 新增 Thread。
     *
     * @param thread Thread 实体
     * @return 影响行数
     */
    int insert(AgentThread thread);

    /**
     * 更新 Thread 的摘要和长期记忆。
     *
     * @param thread Thread 实体
     * @return 影响行数
     */
    int update(AgentThread thread);

    /**
     * 按主键查询 Thread。
     *
     * @param id Thread 标识
     * @return Thread
     */
    Optional<AgentThread> findById(String id);

    /**
     * 按标的类型和代码查询 Thread。
     *
     * @param subjectType 标的类型
     * @param subjectCode 标的代码
     * @return Thread
     */
    Optional<AgentThread> findBySubject(@Param("subjectType") SubjectType subjectType,
                                        @Param("subjectCode") String subjectCode);
}

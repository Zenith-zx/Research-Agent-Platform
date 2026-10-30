package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.agent.macro.model.MacroRecord;
import com.selfinvestor.earnings.agent.macro.model.MacroState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 宏观状态与审计记录；事实发布与 regime 更新分别持久化。
 */
@Mapper
public interface MacroRepository {
    /**
     * 初始化 UNKNOWN 状态。
     */
    int insertState(MacroState state);

    /**
     * 读取当前宏观状态。
     */
    Optional<MacroState> findState(String threadId);

    /**
     * 在完成事务中锁定状态，避免并发 writer。
     */
    Optional<MacroState> lockState(String threadId);

    /**
     * 只更新服务层白名单内的变化维度，并校验版本。
     */
    int patchState(@Param("threadId") String threadId, @Param("version") long version,
                   @Param("changes") Map<String, String> changes, @Param("now") LocalDateTime now);

    /**
     * 保存一次结构化分析，turnId 唯一以阻止重复提交。
     */
    int insertAnalysis(MacroRecord record);

    /**
     * 返回最近五次分析摘要与变更。
     */
    List<MacroRecord> recentAnalyses(String threadId);

    /**
     * 返回最近五次实质 regime 变化，不让无变更分析挤掉重要历史。
     */
    List<MacroRecord> recentChanges(String threadId);

    /**
     * 按 Turn 查询结果。
     */
    Optional<MacroRecord> findAnalysis(String turnId);

    /**
     * 持久化不可变的带来源发布数据。
     */
    int insertRelease(@Param("id") String id, @Param("indicator") String indicator,
                      @Param("period") String period, @Param("date") LocalDateTime date,
                      @Param("json") String json);

    /**
     * 读取指定发布数据。
     */
    Optional<String> findRelease(String id);

    /**
     * 返回指定指标有限历史，可选择一个 period，不跨未来发布时间查询。
     */
    List<String> releaseHistory(@Param("indicator") String indicator, @Param("period") String period,
                               @Param("asOf") LocalDateTime asOf, @Param("limit") int limit);
}

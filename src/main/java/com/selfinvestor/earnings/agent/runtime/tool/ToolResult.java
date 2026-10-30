package com.selfinvestor.earnings.agent.runtime.tool;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool 执行后返回给 Agent Loop 的 Observation。
 */
@Data
@NoArgsConstructor
public class ToolResult {
    private boolean success;
    private Object data;
    private String error;
    private long durationMillis;

    /**
     * 创建成功结果。
     *
     * @param data Tool 数据
     * @param durationMillis 执行耗时
     * @return 成功结果
     */
    public static ToolResult success(Object data, long durationMillis) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        result.setDurationMillis(durationMillis);
        return result;
    }

    /**
     * 创建失败结果。
     *
     * @param error 错误说明
     * @param durationMillis 执行耗时
     * @return 失败结果
     */
    public static ToolResult failure(String error, long durationMillis) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setError(error);
        result.setDurationMillis(durationMillis);
        return result;
    }
}

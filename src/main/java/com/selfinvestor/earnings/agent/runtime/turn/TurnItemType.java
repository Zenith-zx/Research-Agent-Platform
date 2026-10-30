package com.selfinvestor.earnings.agent.runtime.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Turn 内可持久化的事件类型。
 */
@Getter
@AllArgsConstructor
public enum TurnItemType {
    USER_INPUT("用户输入"),
    TRIGGER_INPUT("外部业务触发输入"),
    MODEL_DECISION("模型的结构化决策摘要"),
    TOOL_CALL("Tool 调用请求"),
    TOOL_RESULT("Tool 执行结果"),
    STEER_INPUT("运行期间追加的引导输入"),
    FINAL_RESPONSE("最终回答"),
    ERROR("运行错误或限制信息");

    /**
     * 事件类型说明。
     */
    private final String desc;
}

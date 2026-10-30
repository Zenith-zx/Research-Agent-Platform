package com.selfinvestor.earnings.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户启动或引导 Agent Turn 的输入。
 */
@Data
public class AgentInputRequestDTO {
    /**
     * 用户希望 Agent 处理的文本。
     */
    @NotBlank
    private String text;
    /**
     * 可选的明确研究目标；为空时使用 text。
     */
    private String goal;
}

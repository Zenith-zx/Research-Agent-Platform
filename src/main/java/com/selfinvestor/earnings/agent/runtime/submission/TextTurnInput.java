package com.selfinvestor.earnings.agent.runtime.submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通用户文本输入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextTurnInput implements TurnInput {
    private String text;

    /**
     * 返回原始用户文本。
     *
     * @return 用户文本
     */
    @Override
    public String asText() {
        return text;
    }
}

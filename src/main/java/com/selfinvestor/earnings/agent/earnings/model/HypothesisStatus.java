package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 研究假设经过 Evidence 验证后的状态。
 */
@Getter
@AllArgsConstructor
public enum HypothesisStatus {
    UNTESTED("假设尚未经过足够证据验证"),
    SUPPORTED("现有证据整体支持该假设"),
    REJECTED("现有证据明确反驳该假设"),
    INCONCLUSIVE("证据不足或相互冲突，暂时无法确认或否定该假设");

    /**
     * 假设状态说明。
     */
    private final String desc;
}

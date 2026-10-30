package com.selfinvestor.earnings.agent.macro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 宏观研究的ConfidenceLevel分类，UNKNOWN 不代表中性。
 */
@Getter
@AllArgsConstructor
public enum ConfidenceLevel {
    LOW("证据不足或互相矛盾，低置信度"),
    MEDIUM("有一定证据支持但仍存在关键缺口"),
    HIGH("多项可靠证据一致支持判断");

    /** 分类的中文解释。 */
    private final String desc;
}

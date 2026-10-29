package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公司业绩指引相对于可验证市场预期的评价。
 */
@Getter
@AllArgsConstructor
public enum GuidanceAssessment {
    ABOVE_EXPECTATION("公司指引高于财报发布前的可验证市场预期"),
    INLINE("公司指引与财报发布前的可验证市场预期基本一致"),
    BELOW_EXPECTATION("公司指引低于财报发布前的可验证市场预期"),
    NO_GUIDANCE("公司未提供有效指引，或现有资料不足以评价指引");

    /**
     * 指引评价说明。
     */
    private final String desc;
}

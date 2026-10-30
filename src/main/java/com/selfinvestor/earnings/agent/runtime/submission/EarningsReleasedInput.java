package com.selfinvestor.earnings.agent.runtime.submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新财报发布触发的输入信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningsReleasedInput implements TurnInput {
    private String ticker;
    private String companyName;
    private Long filingId;
    private String formType;
    private String filingDate;
    private String secUrl;

    /**
     * 将财报元数据转换为 Agent 可读的触发说明。
     *
     * @return 财报触发文本
     */
    @Override
    public String asText() {
        return "%s (%s) released %s on %s. filingId=%s, source=%s"
                .formatted(companyName, ticker, formType, filingDate, filingId, secUrl);
    }
}

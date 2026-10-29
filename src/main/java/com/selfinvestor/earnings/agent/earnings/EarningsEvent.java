package com.selfinvestor.earnings.agent.earnings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 可持久化的财报 Agent 输入。只包含业务数据，不持有客户端或运行时资源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningsEvent implements Serializable {
    private String ticker;
    private String companyName;
    private Long filingId;
    private String accessionNumber;
    private String formType;
    private LocalDate filingDate;
    private String secUrl;
    private String sourceContent;
}

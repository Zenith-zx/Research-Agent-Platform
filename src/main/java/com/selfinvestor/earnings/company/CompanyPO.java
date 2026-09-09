package com.selfinvestor.earnings.company;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 关注公司实体。
 */
@Data
@NoArgsConstructor
public class CompanyPO {
    /**
     * 公司主键。
     */
    private Long id;
    /**
     * 股票代码。
     */
    private String ticker;
    /**
     * SEC CIK。
     */
    private String cik;
    /**
     * 公司名称。
     */
    private String companyName;
    /**
     * 是否启用监听。
     */
    private boolean enabled = true;
    /**
     * 创建时间。
     */
    private Instant createdAt;
    /**
     * 更新时间。
     */
    private Instant updatedAt;

    /**
     * 创建关注公司。
     *
     * @param ticker 股票代码
     * @param cik SEC CIK
     * @param companyName 公司名称
     * @param enabled 是否启用监听
     */
    public CompanyPO(String ticker, String cik, String companyName, boolean enabled) {
        this.ticker = ticker.toUpperCase();
        this.cik = cik;
        this.companyName = companyName;
        this.enabled = enabled;
    }

}

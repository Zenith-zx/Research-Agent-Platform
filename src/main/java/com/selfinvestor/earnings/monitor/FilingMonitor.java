package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.CompanyPO;

public interface FilingMonitor {
    /**
     * 轮询指定公司的最新 Filing。
     *
     * @param company 关注公司
     */
    void poll(CompanyPO company);
}

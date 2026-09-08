package com.selfinvestor.earnings.monitor;

import com.selfinvestor.earnings.company.Company;

public interface FilingMonitor {
    void poll(Company company);
}

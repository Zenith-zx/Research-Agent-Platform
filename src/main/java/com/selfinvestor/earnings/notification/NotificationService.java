package com.selfinvestor.earnings.notification;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysis;
import com.selfinvestor.earnings.filing.Filing;

public interface NotificationService {
    void send(Filing filing, FinancialAnalysis analysis);
}

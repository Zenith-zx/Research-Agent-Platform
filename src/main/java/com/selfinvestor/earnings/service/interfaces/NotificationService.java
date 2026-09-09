package com.selfinvestor.earnings.service.interfaces;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.filing.FilingPO;

public interface NotificationService {
    /**
     * 发送财报分析通知。
     *
     * @param filing Filing 实体
     * @param analysis 结构化分析结果
     */
    void send(FilingPO filing, FinancialAnalysisDTO analysis);
}

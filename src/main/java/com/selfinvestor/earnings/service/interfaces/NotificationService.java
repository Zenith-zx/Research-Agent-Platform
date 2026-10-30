package com.selfinvestor.earnings.service.interfaces;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.marketstress.MarketStressAnalysisPO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;

public interface NotificationService {
    /**
     * 发送已完成宏观研究；不支持的渠道默认忽略。
     */
    default void sendMacro(String turnId, String report) {
    }
    /**
     * 发送财报分析通知。
     *
     * @param filing Filing 实体
     * @param analysis 结构化分析结果
     */
    void send(FilingPO filing, FinancialAnalysisDTO analysis);

    /**
     * 发送市场压力分析通知；不支持此类消息的渠道可以保留默认实现。
     *
     * @param analysisPO 市场压力持久化记录
     * @param analysis 结构化市场压力分析
     */
    default void sendMarketStress(MarketStressAnalysisPO analysisPO, MarketStressAnalysisDTO analysis) {
    }
}

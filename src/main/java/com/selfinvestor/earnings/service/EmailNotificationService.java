package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.analysis.dto.FinancialAnalysisDTO;
import com.selfinvestor.earnings.config.MarketStressProperties;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.marketstress.MarketStressAnalysisPO;
import com.selfinvestor.earnings.marketstress.dto.MarketStressAnalysisDTO;
import com.selfinvestor.earnings.marketstress.dto.PrimaryDriverDTO;
import com.selfinvestor.earnings.service.interfaces.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 邮件通知渠道。
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "market-stress.email", name = "enabled", havingValue = "true")
public class EmailNotificationService implements NotificationService {
    /**
     * 复用已配置的邮件收件人发送宏观研究，不影响分析提交事务。
     */
    @Override
    public void sendMacro(String turnId, String report) {
        if (marketStressProperties.getEmail().isEnabled()) {
            send("[宏观研究] US_MACRO " + turnId, report);
        }
    }
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private MarketStressProperties marketStressProperties;
    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送原有财报分析邮件。
     *
     * @param filing Filing 实体
     * @param analysis 结构化财报分析
     */
    @Override
    public void send(FilingPO filing, FinancialAnalysisDTO analysis) {
        if (!marketStressProperties.getEmail().isEnabled()) {
            return;
        }
        send("[财报分析] " + filing.getCompany().getTicker() + " " + filing.getFormType(),
                analysis.getSummary() + "\n\n" + filing.getSecUrl());
    }

    /**
     * 发送市场压力分析邮件。
     *
     * @param analysisPO 市场压力持久化记录
     * @param analysis 结构化市场压力分析
     */
    @Override
    public void sendMarketStress(MarketStressAnalysisPO analysisPO, MarketStressAnalysisDTO analysis) {
        if (!marketStressProperties.getEmail().isEnabled()) {
            return;
        }
        String subject = String.format("🚨 [%s超卖] RSI6=%s｜Mean Reversion %s/100｜%s",
                analysisPO.getSymbol(), value(analysisPO.getRsi6()), value(analysis.getMeanReversionScore()), analysis.getAction());
        send(subject, formatMarketStress(analysisPO, analysis));
        log.info("Email notification sent: symbol={} analysisId={}", analysisPO.getSymbol(), analysisPO.getId());
    }

    /**
     * 发送一封简单文本邮件。
     *
     * @param subject 邮件标题
     * @param body 邮件正文
     */
    private void send(String subject, String body) {
        List<String> recipients = marketStressProperties.getEmail().getRecipients();
        if (CollectionUtils.isEmpty(recipients)) {
            throw new IllegalStateException("MARKET_STRESS_EMAIL_RECIPIENTS is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * 格式化市场压力邮件正文。
     *
     * @param po 持久化记录
     * @param analysis 结构化分析
     * @return 邮件正文
     */
    private String formatMarketStress(MarketStressAnalysisPO po, MarketStressAnalysisDTO analysis) {
        return po.getSymbol() + " Market Stress Alert\n\n"
                + "RSI6: " + value(po.getRsi6()) + "\n"
                + "5D Return: " + value(po.getFiveDayReturn()) + "%\n"
                + "VIX: " + value(po.getVix()) + "\n"
                + "10Y: " + value(po.getUs10y()) + "%\n"
                + "Brent: " + value(po.getBrent()) + "\n\n"
                + "市场正在交易：\n" + value(analysis.getMarketIsTrading()) + "\n\n"
                + "Shock:\n" + drivers(analysis.getPrimaryDrivers()) + "\n"
                + "Event Stage: " + analysis.getEventStage() + "\n"
                + "基本面损害: " + analysis.getFundamentalDamage() + "\n"
                + "金融系统压力: " + analysis.getFinancialStress() + "\n"
                + "事件本身定价: " + analysis.getEventPricing() + "\n"
                + "二阶影响定价: " + analysis.getSecondOrderEffectsPricing() + "\n"
                + "Seller Exhaustion: " + analysis.getSellerExhaustion() + "\n"
                + "Mean Reversion Score: " + analysis.getMeanReversionScore() + " / 100\n"
                + "Action: " + analysis.getAction() + "\n\n"
                + "需要观察：\n" + numbered(analysis.getConfirmationSignals()) + "\n"
                + "当前风险：\n" + numbered(analysis.getInvalidationSignals()) + "\n"
                + "AI总结：\n" + value(analysis.getReasoningSummary());
    }

    /**
     * 将列表格式化为编号文本。
     *
     * @param values 文本列表
     * @return 编号文本
     */
    private String numbered(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "N/A\n";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            builder.append(index + 1).append(". ").append(values.get(index)).append('\n');
        }
        return builder.toString();
    }

    /**
     * 格式化主要市场冲击类型。
     *
     * @param drivers 主要驱动因素
     * @return 冲击类型文本
     */
    private String drivers(List<PrimaryDriverDTO> drivers) {
        if (CollectionUtils.isEmpty(drivers)) {
            return "UNKNOWN\n";
        }
        StringBuilder builder = new StringBuilder();
        for (PrimaryDriverDTO driver : drivers) {
            builder.append(driver.getType()).append(" (").append(driver.getImportance()).append(")\n");
        }
        return builder.toString();
    }

    /**
     * 将空值格式化为 N/A。
     *
     * @param value 原始值
     * @return 展示值
     */
    private String value(Object value) {
        return value == null ? "N/A" : value.toString();
    }
}

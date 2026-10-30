package com.selfinvestor.earnings.agent.runtime.tool;

import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询项目数据库中同一公司的历史 Filing。
 */
@Component
public class HistoricalFilingsTool implements AgentTool {
    @Resource
    private FilingRepository filings;

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "get_historical_filings";
    }

    /**
     * 返回 Tool 能力说明。
     *
     * @return 能力说明
     */
    @Override
    public String description() {
        return "查询同一 ticker 最近的 SEC Filing 元数据，可据此选择历史 filingId 再读取正文。";
    }

    /**
     * 返回 ticker 和 limit 参数 Schema。
     *
     * @return 参数 Schema
     */
    @Override
    public ToolSchema schema() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("ticker", "股票代码；不传时使用当前 Thread 标的");
        properties.put("limit", "返回数量，范围 1 到 20，默认 8");
        return new ToolSchema(properties, List.of());
    }

    /**
     * 查询并裁剪历史 Filing 字段。
     *
     * @param context Agent 上下文
     * @param arguments Tool 参数
     * @return 历史 Filing 列表
     */
    @Override
    public Object execute(AgentToolContext context, Map<String, Object> arguments) {
        String ticker = arguments.getOrDefault("ticker", context.getThread().getSubjectCode()).toString();
        int limit = integerArgument(arguments.get("limit"), 8);
        return filings.findRecentByTicker(ticker, Math.max(1, Math.min(20, limit))).stream()
                .map(this::summary)
                .toList();
    }

    /**
     * 将 Filing 转换为 Tool 需要的摘要字段。
     *
     * @param filing Filing 实体
     * @return Filing 摘要
     */
    private Map<String, Object> summary(FilingPO filing) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("filingId", filing.getId());
        value.put("formType", filing.getFormType());
        value.put("filingDate", filing.getFilingDate());
        value.put("status", filing.getStatus());
        value.put("secUrl", filing.getSecUrl());
        return value;
    }

    /**
     * 读取整数参数。
     *
     * @param value 原始参数
     * @param fallback 默认值
     * @return 整数值
     */
    private int integerArgument(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }
}

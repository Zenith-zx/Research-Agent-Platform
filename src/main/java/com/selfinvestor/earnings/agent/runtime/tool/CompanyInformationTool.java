package com.selfinvestor.earnings.agent.runtime.tool;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.service.CompanyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用现有 CompanyService 查询关注公司基础资料。
 */
@Component
public class CompanyInformationTool implements AgentTool {
    @Resource
    private CompanyService companies;

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "get_company_information";
    }

    /**
     * 返回 Tool 能力说明。
     *
     * @return 能力说明
     */
    @Override
    public String description() {
        return "查询项目中已关注公司的名称、ticker、CIK 和监听状态。";
    }

    /**
     * 返回 ticker 参数 Schema。
     *
     * @return 参数 Schema
     */
    @Override
    public ToolSchema schema() {
        return new ToolSchema(Map.of("ticker", "股票代码；不传时使用当前 Thread 标的"), List.of());
    }

    /**
     * 查询公司并返回安全字段。
     *
     * @param context Agent 上下文
     * @param arguments Tool 参数
     * @return 公司字段
     */
    @Override
    public Object execute(AgentToolContext context, Map<String, Object> arguments) {
        String ticker = stringArgument(arguments, "ticker", context.getThread().getSubjectCode());
        CompanyPO company = companies.findByTicker(ticker);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticker", company.getTicker());
        result.put("companyName", company.getCompanyName());
        result.put("cik", company.getCik());
        result.put("monitorEnabled", company.isEnabled());
        return result;
    }

    /**
     * 读取可选字符串参数。
     *
     * @param arguments 参数集合
     * @param name 参数名
     * @param fallback 默认值
     * @return 参数文本
     */
    private String stringArgument(Map<String, Object> arguments, String name, String fallback) {
        Object value = arguments.get(name);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}

package com.selfinvestor.earnings.config;

import com.selfinvestor.earnings.agent.macro.*;
import com.selfinvestor.earnings.agent.runtime.tool.*;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

/**
 * 将现有事实服务注册为独立工具，Profile 决定可见性而非固定调用顺序。
 */
@Configuration
public class MacroToolsConfiguration {
    @Resource
    private MacroFacts facts;

    /**
     * 读取已导入的宏观发布；含来源、时间、actual/consensus/previous/revision。未接官方自动源。
     */
    @Bean
    public AgentTool getMacroRelease() {
        return new MacroFactTool("get_macro_release", "读取已导入的宏观发布；含来源、时间、actual/consensus/previous/revision。未接官方自动源。",
                new ToolSchema(Map.of("releaseId", "发布 ID；新 Steer 事件请显式指定", "indicator", "CPI/NFP/FOMC 等", "period", "例如 2026-09"), List.of()), facts);
    }

    /**
     * 读取最多 24 份导入历史，可能不连续，可能包含修订版本；无档案时明确不可用。
     */
    @Bean
    public AgentTool getMacroHistory() {
        return new MacroFactTool("get_macro_history", "读取最多 24 份导入历史，可能不连续，可能包含修订版本；无档案时明确不可用。",
                new ToolSchema(Map.of("indicator", "CPI/NFP/PCE/PMI 等；子指标在 readings 内", "limit", "1-24，默认 12"), List.of()), facts);
    }

    /**
     * 仅返回导入的 CPI 分项；没有分项时明确不可用。
     */
    @Bean
    public AgentTool getCpiComponents() {
        return new MacroFactTool("get_cpi_components", "仅返回导入的 CPI 分项；没有分项时明确不可用。",
                new ToolSchema(Map.of("releaseId", "CPI 发布 ID"), List.of()), facts);
    }

    /**
     * 读取已导入的 FOMC 声明、利率决定、SEP 和 Powell 发言（若提供）。
     */
    @Bean
    public AgentTool getFomcStatement() {
        return new MacroFactTool("get_fomc_statement", "读取已导入的 FOMC 声明、利率决定、SEP 和 Powell 发言（若提供）。",
                new ToolSchema(Map.of("releaseId", "FOMC 发布 ID", "period", "可选会议 period"), List.of()), facts);
    }

    /**
     * 对照导入的本次/前次声明原文，由 Agent 解释变化。
     */
    @Bean
    public AgentTool comparePreviousFomcStatement() {
        return new MacroFactTool("compare_previous_fomc_statement", "对照导入的本次/前次声明原文，由 Agent 解释变化。",
                new ToolSchema(Map.of("releaseId", "FOMC 发布 ID", "period", "可选会议 period"), List.of()), facts);
    }

    /**
     * 查询 2Y/5Y/10Y/30Y 与同时间口径的 2s10s/5s30s。现有 Finnhub 无此 feed，会返回缺失。
     */
    @Bean
    public AgentTool getTreasuryYields() {
        return new MacroFactTool("get_treasury_yields", "查询 2Y/5Y/10Y/30Y 与同时间口径的 2s10s/5s30s。现有 Finnhub 无此 feed，会返回缺失。",
                new ToolSchema(Map.of(), List.of()), facts);
    }

    /**
     * 查询 DXY；现有 Provider 无数据源时显式不可用，不虚构趋势。
     */
    @Bean
    public AgentTool getDollarIndex() {
        return new MacroFactTool("get_dollar_index", "查询 DXY；现有 Provider 无数据源时显式不可用，不虚构趋势。",
                new ToolSchema(Map.of(), List.of()), facts);
    }

    /**
     * 复用已有 Finnhub VIX 行情；取决于供应商权限及配置。
     */
    @Bean
    public AgentTool getVix() {
        return new MacroFactTool("get_vix", "复用已有 Finnhub VIX 行情；取决于供应商权限及配置。",
                new ToolSchema(Map.of(), List.of()), facts);
    }

    /**
     * 查询 SPY/QQQ/IWM 的当前快照和本地历史；不是严格事件前后窗口行情。
     */
    @Bean
    public AgentTool getMarketIndex() {
        return new MacroFactTool("get_market_index", "查询 SPY/QQQ/IWM 的当前快照和本地历史；不是严格事件前后窗口行情。",
                new ToolSchema(Map.of("symbol", "SPY、QQQ 或 IWM，默认 QQQ"), List.of()), facts);
    }

    /**
     * 查询 WTI；现有 Provider 不支持则返回不可用。
     */
    @Bean
    public AgentTool getOilPrice() {
        return new MacroFactTool("get_oil_price", "查询 WTI；现有 Provider 不支持则返回不可用。",
                new ToolSchema(Map.of(), List.of()), facts);
    }

    /**
     * 复用 Finnhub 最近 48 小时市场新闻，最多 12 条；仅作辅助，不替代官方数据。
     */
    @Bean
    public AgentTool getMacroNews() {
        return new MacroFactTool("get_macro_news", "复用 Finnhub 最近 48 小时市场新闻，最多 12 条；仅作辅助，不替代官方数据。",
                new ToolSchema(Map.of(), List.of()), facts);
    }

}

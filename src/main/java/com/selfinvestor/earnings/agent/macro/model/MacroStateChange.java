package com.selfinvestor.earnings.agent.macro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 稀疏变更，null 表示该维度无变化，禁止整份模型状态覆盖数据库。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MacroStateChange {
    private RegimeChange<GrowthRegime> growth;
    private RegimeChange<InflationRegime> inflation;
    private RegimeChange<LaborRegime> labor;
    private RegimeChange<FedRegime> fed;
    private RegimeChange<LiquidityRegime> liquidity;
    private RegimeChange<RatesRegime> rates;
    private RegimeChange<DollarRegime> dollar;
    private RegimeChange<RiskRegime> risk;
}

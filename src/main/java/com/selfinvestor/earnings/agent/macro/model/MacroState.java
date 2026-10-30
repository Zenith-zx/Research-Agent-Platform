package com.selfinvestor.earnings.agent.macro.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 长期宏观环境；缺少依据的维度保持 UNKNOWN，不把单次读数视为 regime。
 */
@Data
public class MacroState {
    private String threadId;
    private GrowthRegime growth = GrowthRegime.UNKNOWN;
    private InflationRegime inflation = InflationRegime.UNKNOWN;
    private LaborRegime labor = LaborRegime.UNKNOWN;
    private FedRegime fed = FedRegime.UNKNOWN;
    private LiquidityRegime liquidity = LiquidityRegime.UNKNOWN;
    private RatesRegime rates = RatesRegime.UNKNOWN;
    private DollarRegime dollar = DollarRegime.UNKNOWN;
    private RiskRegime risk = RiskRegime.UNKNOWN;
    private long version;
    private LocalDateTime updatedAt;
}

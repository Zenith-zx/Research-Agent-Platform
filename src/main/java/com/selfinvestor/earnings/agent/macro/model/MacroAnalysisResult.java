package com.selfinvestor.earnings.agent.macro.model;

import lombok.Data;
import java.util.List;

/**
 * 最终宏观研究结果；事实、市场反应、传导推论分开，不包含交易指令。
 */
@Data
public class MacroAnalysisResult {
    private String summary;
    private String whatChanged;
    private MacroStateChange proposedStateChange;
    private List<String> keyFindings;
    private List<String> marketImpacts;
    private String marketReaction;
    private List<String> researchImplications;
    private List<String> risks;
    private ConfidenceLevel confidence;
}

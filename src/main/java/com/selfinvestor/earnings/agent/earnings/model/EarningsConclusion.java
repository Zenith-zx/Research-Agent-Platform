package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningsConclusion implements Serializable {
    private FundamentalTrend fundamentalTrend;
    private GrowthTrend growthTrend;
    private MarginTrend marginTrend;
    private GuidanceAssessment guidanceAssessment;
    private ExpectationGap expectationGap;
    private EarningsQuality earningsQuality;
    private double confidence;
    private List<String> positiveChanges = new ArrayList<>();
    private List<String> negativeChanges = new ArrayList<>();
    private List<String> keyRisks = new ArrayList<>();
    private List<String> nextQuarterWatchList = new ArrayList<>();
    private String executiveSummary;
}

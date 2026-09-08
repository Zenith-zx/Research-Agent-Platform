package com.selfinvestor.earnings.analysis.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialAnalysis(
        String company,
        String ticker,
        String fiscalQuarter,
        String headline,
        Financials financials,
        Guidance guidance,
        List<Segment> segments,
        List<String> positives,
        List<String> negatives,
        List<String> risks,
        List<String> managementCommentary,
        FundamentalTrend fundamentalTrend,
        String fundamentalTrendReason,
        boolean investmentThesisChanged,
        String investmentThesisChangeReason,
        boolean consensusComparisonAvailable,
        String consensusComparisonNote,
        Integer qualityScore,
        String summary) {

    public record Metric(String value, BigDecimal yoyGrowth, BigDecimal qoqGrowth) {}
    public record Margin(String value, String previousQuarter, String trend) {}
    public record Financials(
            Metric revenue,
            Margin grossMargin,
            Margin operatingMargin,
            String operatingIncome,
            String netIncome,
            String eps,
            String freeCashFlow,
            String capex,
            String cash,
            String debt) {}
    public record Guidance(
            String nextQuarterRevenueLow,
            String nextQuarterRevenueHigh,
            String grossMargin,
            String fullYearGuidance) {}
    public record Segment(String name, String revenue, BigDecimal yoyGrowth, BigDecimal qoqGrowth, String commentary) {}
}

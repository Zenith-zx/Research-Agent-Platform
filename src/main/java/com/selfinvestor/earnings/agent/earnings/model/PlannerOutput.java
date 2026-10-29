package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlannerOutput {
    private String fiscalQuarter;
    private ResearchPlan researchPlan;
    private List<ResearchTask> researchTasks = new ArrayList<>();
    private List<Hypothesis> initialHypotheses = new ArrayList<>();
}

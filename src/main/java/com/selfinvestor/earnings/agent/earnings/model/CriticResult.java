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
public class CriticResult implements Serializable {
    private CriticDecision decision;
    private double evidenceQuality;
    private double confidence;
    private List<String> missingInformation = new ArrayList<>();
    private List<String> contradictions = new ArrayList<>();
    private List<String> weakClaims = new ArrayList<>();
    private List<ResearchTask> suggestedTasks = new ArrayList<>();
}

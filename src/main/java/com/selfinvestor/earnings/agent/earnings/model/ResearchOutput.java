package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResearchOutput {
    private List<Evidence> evidence = new ArrayList<>();
    private List<Hypothesis> hypotheses = new ArrayList<>();
    private List<OpenQuestion> openQuestions = new ArrayList<>();
}

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
public class ResearchPlan implements Serializable {
    private String objective;
    private List<ResearchCategory> focusAreas = new ArrayList<>();
    private String rationale;
}

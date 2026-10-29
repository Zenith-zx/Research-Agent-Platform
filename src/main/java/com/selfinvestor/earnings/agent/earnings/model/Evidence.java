package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evidence implements Serializable {
    private String evidenceId;
    private String taskId;
    private String claim;
    private String metric;
    private String value;
    private String comparison;
    private String source;
    private EvidenceSourceType sourceType;
    private double confidence;
}

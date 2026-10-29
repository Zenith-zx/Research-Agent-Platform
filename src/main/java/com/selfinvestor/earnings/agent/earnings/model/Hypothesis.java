package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hypothesis implements Serializable {
    private String hypothesisId;
    private String statement;
    private HypothesisStatus status;
    private double confidence;
}

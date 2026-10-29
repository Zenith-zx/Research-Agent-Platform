package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestion implements Serializable {
    private ResearchCategory category;
    private String question;
    private String reason;
}

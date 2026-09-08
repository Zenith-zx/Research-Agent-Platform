package com.selfinvestor.earnings.analysis;

import com.selfinvestor.earnings.filing.Filing;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_analysis", uniqueConstraints = @UniqueConstraint(name = "uk_analysis_filing", columnNames = "filing_id"))
public class AiAnalysis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filing_id", nullable = false)
    private Filing filing;
    @Column(nullable = false, length = 100)
    private String model;
    @Lob @Column(name = "analysis_json", nullable = false, columnDefinition = "LONGTEXT")
    private String analysisJson;
    @Lob @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiAnalysis() {}
    public AiAnalysis(Filing filing, String model, String analysisJson, String summary) {
        this.filing = filing; this.model = model; this.analysisJson = analysisJson; this.summary = summary;
    }
    @PrePersist void prePersist() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Filing getFiling() { return filing; }
    public String getModel() { return model; }
    public String getAnalysisJson() { return analysisJson; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
}

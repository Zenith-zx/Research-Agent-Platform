package com.selfinvestor.earnings.filing;

import jakarta.persistence.*;

@Entity
@Table(name = "filing_document", uniqueConstraints = @UniqueConstraint(name = "uk_filing_document", columnNames = {"filing_id", "document_name"}))
public class FilingDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filing_id", nullable = false)
    private Filing filing;
    @Column(name = "document_type", length = 64)
    private String documentType;
    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, length = 1024)
    private String url;
    @Lob @Column(name = "raw_content", columnDefinition = "LONGTEXT")
    private String rawContent;
    @Lob @Column(name = "clean_content", columnDefinition = "LONGTEXT")
    private String cleanContent;
    @Column(name = "selected_for_analysis", nullable = false)
    private boolean selectedForAnalysis;

    protected FilingDocument() {}
    public FilingDocument(Filing filing, String documentType, String documentName, String description, String url, String rawContent) {
        this.filing = filing; this.documentType = documentType; this.documentName = documentName;
        this.description = description; this.url = url; this.rawContent = rawContent;
    }
    public void parsed(String cleanContent, boolean selected) { this.cleanContent = cleanContent; this.selectedForAnalysis = selected; }
    public Long getId() { return id; }
    public Filing getFiling() { return filing; }
    public String getDocumentType() { return documentType; }
    public String getDocumentName() { return documentName; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public String getRawContent() { return rawContent; }
    public String getCleanContent() { return cleanContent; }
    public boolean isSelectedForAnalysis() { return selectedForAnalysis; }
}

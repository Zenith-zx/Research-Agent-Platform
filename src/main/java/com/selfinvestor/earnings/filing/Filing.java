package com.selfinvestor.earnings.filing;

import com.selfinvestor.earnings.company.Company;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "filing", uniqueConstraints = @UniqueConstraint(name = "uk_filing_accession", columnNames = "accession_number"))
public class Filing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(name = "accession_number", nullable = false, length = 32)
    private String accessionNumber;
    @Column(name = "form_type", nullable = false, length = 20)
    private String formType;
    @Column(name = "filing_date", nullable = false)
    private LocalDate filingDate;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Column(name = "primary_document", length = 255)
    private String primaryDocument;
    @Column(name = "sec_url", nullable = false, length = 1024)
    private String secUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private FilingStatus status;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Filing() {}

    public Filing(Company company, String accessionNumber, String formType, LocalDate filingDate,
                  Instant acceptedAt, String primaryDocument, String secUrl) {
        this.company = company;
        this.accessionNumber = accessionNumber;
        this.formType = formType;
        this.filingDate = filingDate;
        this.acceptedAt = acceptedAt;
        this.primaryDocument = primaryDocument;
        this.secUrl = secUrl;
        this.status = FilingStatus.DETECTED;
    }

    @PrePersist void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public void transitionTo(FilingStatus status) { this.status = status; this.errorMessage = null; }
    public void fail(String message) { this.status = FilingStatus.FAILED; this.errorMessage = message == null ? "Unknown error" : message.substring(0, Math.min(2000, message.length())); }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public String getAccessionNumber() { return accessionNumber; }
    public String getFormType() { return formType; }
    public LocalDate getFilingDate() { return filingDate; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public String getPrimaryDocument() { return primaryDocument; }
    public String getSecUrl() { return secUrl; }
    public FilingStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
}

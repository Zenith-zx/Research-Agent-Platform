package com.selfinvestor.earnings.company;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "company")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 16)
    private String ticker;
    @Column(nullable = false, unique = true, length = 10)
    private String cik;
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {}

    public Company(String ticker, String cik, String companyName, boolean enabled) {
        this.ticker = ticker.toUpperCase();
        this.cik = cik;
        this.companyName = companyName;
        this.enabled = enabled;
    }

    @PrePersist void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public String getCik() { return cik; }
    public String getCompanyName() { return companyName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

CREATE TABLE company (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticker VARCHAR(16) NOT NULL,
    cik VARCHAR(10) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_company_ticker UNIQUE (ticker),
    CONSTRAINT uk_company_cik UNIQUE (cik)
);

CREATE TABLE filing (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    accession_number VARCHAR(32) NOT NULL,
    form_type VARCHAR(20) NOT NULL,
    filing_date DATE NOT NULL,
    accepted_at TIMESTAMP(6) NULL,
    primary_document VARCHAR(255) NULL,
    sec_url VARCHAR(1024) NOT NULL,
    status VARCHAR(24) NOT NULL,
    error_message VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_filing_accession UNIQUE (accession_number),
    CONSTRAINT fk_filing_company FOREIGN KEY (company_id) REFERENCES company(id),
    INDEX idx_filing_company_date (company_id, filing_date),
    INDEX idx_filing_status (status)
);

CREATE TABLE filing_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    filing_id BIGINT NOT NULL,
    document_type VARCHAR(64) NULL,
    document_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    url VARCHAR(1024) NOT NULL,
    raw_content LONGTEXT NULL,
    clean_content LONGTEXT NULL,
    selected_for_analysis BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_filing_document UNIQUE (filing_id, document_name),
    CONSTRAINT fk_document_filing FOREIGN KEY (filing_id) REFERENCES filing(id) ON DELETE CASCADE
);

CREATE TABLE ai_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    filing_id BIGINT NOT NULL,
    model VARCHAR(100) NOT NULL,
    analysis_json LONGTEXT NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_analysis_filing UNIQUE (filing_id),
    CONSTRAINT fk_analysis_filing FOREIGN KEY (filing_id) REFERENCES filing(id) ON DELETE CASCADE
);

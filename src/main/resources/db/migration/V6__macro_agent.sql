CREATE TABLE MACRO_STATE (
    thread_id VARCHAR(36) PRIMARY KEY,
    growth VARCHAR(32) NOT NULL,
    inflation VARCHAR(32) NOT NULL,
    labor VARCHAR(32) NOT NULL,
    fed VARCHAR(32) NOT NULL,
    liquidity VARCHAR(32) NOT NULL,
    rates VARCHAR(32) NOT NULL,
    dollar VARCHAR(32) NOT NULL,
    risk VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_macro_state_thread FOREIGN KEY (thread_id) REFERENCES AGENT_THREAD(id)
);
CREATE TABLE MACRO_ANALYSIS (
    turn_id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36) NOT NULL,
    result_json LONGTEXT NOT NULL,
    changes_json LONGTEXT NOT NULL,
    summary TEXT NOT NULL,
    state_version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_macro_analysis_turn FOREIGN KEY (turn_id) REFERENCES AGENT_TURN(id),
    CONSTRAINT fk_macro_analysis_thread FOREIGN KEY (thread_id) REFERENCES AGENT_THREAD(id)
);
CREATE INDEX idx_macro_analysis_thread_time ON MACRO_ANALYSIS(thread_id, created_at);
CREATE TABLE MACRO_RELEASE (
    release_id VARCHAR(100) PRIMARY KEY,
    indicator VARCHAR(32) NOT NULL,
    period VARCHAR(32) NOT NULL,
    release_date DATETIME(6) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL
);
CREATE INDEX idx_macro_release_indicator_date ON MACRO_RELEASE(indicator, release_date);

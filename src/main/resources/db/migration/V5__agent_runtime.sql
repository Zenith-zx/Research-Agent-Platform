CREATE TABLE AGENT_THREAD (
    id VARCHAR(36) NOT NULL,
    subject_type VARCHAR(24) NOT NULL,
    subject_code VARCHAR(32) NOT NULL,
    summary TEXT NULL,
    memory LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_AGENT_THREAD_SUBJECT UNIQUE (subject_type, subject_code)
);

CREATE TABLE AGENT_TURN (
    id VARCHAR(36) NOT NULL,
    thread_id VARCHAR(36) NOT NULL,
    submission_id VARCHAR(36) NOT NULL,
    submission_metadata_json LONGTEXT NULL,
    status VARCHAR(24) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    goal TEXT NOT NULL,
    step_count INT NOT NULL,
    llm_call_count INT NOT NULL,
    max_steps INT NOT NULL,
    max_llm_calls INT NOT NULL,
    final_response LONGTEXT NULL,
    failure_message VARCHAR(2000) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_AGENT_TURN_THREAD FOREIGN KEY (thread_id) REFERENCES AGENT_THREAD(id) ON DELETE CASCADE,
    INDEX IDX_AGENT_TURN_THREAD_STATUS (thread_id, status),
    INDEX IDX_AGENT_TURN_CREATED (created_at)
);

CREATE TABLE AGENT_TURN_ITEM (
    id VARCHAR(36) NOT NULL,
    turn_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    content LONGTEXT NULL,
    tool_name VARCHAR(100) NULL,
    call_id VARCHAR(36) NULL,
    payload_json LONGTEXT NULL,
    sequence_no INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_AGENT_TURN_ITEM_TURN FOREIGN KEY (turn_id) REFERENCES AGENT_TURN(id) ON DELETE CASCADE,
    CONSTRAINT UK_AGENT_TURN_ITEM_SEQUENCE UNIQUE (turn_id, sequence_no),
    INDEX IDX_AGENT_TURN_ITEM_TURN (turn_id, sequence_no)
);

CREATE TABLE job_execution_state (
    job_name VARCHAR(100) PRIMARY KEY,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    last_duration_ms BIGINT,
    last_error TEXT,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_execution_state_last_success ON job_execution_state(last_success_at);

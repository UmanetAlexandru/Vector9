CREATE TABLE downstream_events (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    job_name VARCHAR(100),
    environment_name VARCHAR(50) NOT NULL,
    event_at TIMESTAMP NOT NULL,
    message_text TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_downstream_events_status ON downstream_events(status);
CREATE INDEX idx_downstream_events_event_type ON downstream_events(event_type);
CREATE INDEX idx_downstream_events_event_at ON downstream_events(event_at);

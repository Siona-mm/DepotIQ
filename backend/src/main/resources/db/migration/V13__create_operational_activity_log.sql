CREATE TABLE operational_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    activity_type VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id BIGINT NOT NULL,
    reference_label VARCHAR(150) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_operational_activity_logs_created_at
    ON operational_activity_logs (created_at DESC);

CREATE TABLE import_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    import_type VARCHAR(50) NOT NULL,
    processed_rows INTEGER NOT NULL,
    created_records INTEGER NOT NULL,
    updated_records INTEGER NOT NULL,
    skipped_rows INTEGER NOT NULL,
    created_stores INTEGER NOT NULL,
    created_products INTEGER NOT NULL,
    error_summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_import_audit_logs_created_at ON import_audit_logs (created_at DESC);

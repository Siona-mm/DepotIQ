ALTER TABLE sales_records
    ADD COLUMN source_system VARCHAR(100),
    ADD COLUMN external_record_id VARCHAR(200),
    ADD COLUMN imported_at TIMESTAMP;

CREATE UNIQUE INDEX uq_sales_records_external_source
    ON sales_records (source_system, external_record_id)
    WHERE source_system IS NOT NULL
      AND external_record_id IS NOT NULL;

CREATE INDEX idx_sales_records_imported_at
    ON sales_records (imported_at DESC);

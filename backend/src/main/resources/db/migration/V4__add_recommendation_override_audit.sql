ALTER TABLE shipment_recommendations
    ADD COLUMN IF NOT EXISTS original_recommended_shipment INTEGER,
    ADD COLUMN IF NOT EXISTS override_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS overridden_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS overridden_at TIMESTAMP WITH TIME ZONE;

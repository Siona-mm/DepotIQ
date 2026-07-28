ALTER TABLE shipment_recommendations
    ADD COLUMN original_recommended_shipment INTEGER,
    ADD COLUMN override_reason VARCHAR(500),
    ADD COLUMN overridden_by VARCHAR(100),
    ADD COLUMN overridden_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE stores
    ADD COLUMN external_store_id VARCHAR(100);

CREATE UNIQUE INDEX uk_stores_external_store_id
    ON stores (LOWER(external_store_id))
    WHERE external_store_id IS NOT NULL;

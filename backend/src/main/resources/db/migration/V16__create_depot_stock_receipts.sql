CREATE TABLE depot_stock_receipts (
    receipt_id VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id),
    units_received INTEGER NOT NULL CHECK (units_received > 0),
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (receipt_id, product_id)
);

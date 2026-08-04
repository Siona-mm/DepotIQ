CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    shipment_number VARCHAR(50) NOT NULL UNIQUE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    planned_dispatch_date DATE NOT NULL,
    expected_delivery_date DATE NOT NULL,
    dispatched_at TIMESTAMP,
    delivered_at TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shipment_delivery_after_dispatch
        CHECK (expected_delivery_date >= planned_dispatch_date)
);

CREATE TABLE shipment_items (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    recommendation_id BIGINT NOT NULL UNIQUE
        REFERENCES shipment_recommendations(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shipment_item_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_shipments_store_id ON shipments(store_id);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_planned_dispatch_date
    ON shipments(planned_dispatch_date);
CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
CREATE INDEX idx_shipment_items_product_id ON shipment_items(product_id);

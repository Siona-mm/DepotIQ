CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    store_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    store_type VARCHAR(50) NOT NULL,
    region VARCHAR(100),
    has_warehouse BOOLEAN NOT NULL DEFAULT FALSE,
    storage_capacity INTEGER NOT NULL DEFAULT 0,
    delivery_lead_time_days INTEGER NOT NULL DEFAULT 0,
    preferred_horizon_days INTEGER NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    supplier_code VARCHAR(100),
    unit_cost NUMERIC(12, 2),
    price NUMERIC(12, 2),
    weight_kg NUMERIC(10, 3),
    shelf_life_days INTEGER,
    perishable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE store_inventory (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    inventory_level INTEGER NOT NULL DEFAULT 0,
    incoming_units INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_store_inventory_store_product UNIQUE (store_id, product_id)
);

CREATE TABLE depot_inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    available_units INTEGER NOT NULL DEFAULT 0,
    reserved_units INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_depot_inventory_product UNIQUE (product_id)
);

CREATE TABLE sales_records (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sale_date DATE NOT NULL,
    units_sold INTEGER NOT NULL DEFAULT 0,
    price NUMERIC(12, 2),
    discount NUMERIC(5, 2),
    promotion BOOLEAN NOT NULL DEFAULT FALSE,
    weather_condition VARCHAR(100),
    temperature NUMERIC(6, 2),
    holiday_promotion BOOLEAN NOT NULL DEFAULT FALSE,
    seasonality VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sales_store_product_date UNIQUE (store_id, product_id, sale_date)
);

CREATE TABLE demand_forecasts (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    forecast_date DATE NOT NULL,
    horizon_days INTEGER NOT NULL,
    predicted_demand NUMERIC(12, 2) NOT NULL,
    confidence_lower NUMERIC(12, 2),
    confidence_upper NUMERIC(12, 2),
    model_name VARCHAR(150),
    model_version VARCHAR(100),
    model_mae NUMERIC(12, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_forecast_store_product_date_horizon UNIQUE (
        store_id,
        product_id,
        forecast_date,
        horizon_days
    )
);

CREATE TABLE shipment_recommendations (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    demand_forecast_id BIGINT REFERENCES demand_forecasts(id) ON DELETE SET NULL,
    recommendation_date DATE NOT NULL,
    horizon_days INTEGER NOT NULL,
    predicted_demand NUMERIC(12, 2) NOT NULL,
    confidence_lower NUMERIC(12, 2),
    confidence_upper NUMERIC(12, 2),
    current_inventory INTEGER NOT NULL DEFAULT 0,
    incoming_units INTEGER NOT NULL DEFAULT 0,
    safety_stock INTEGER NOT NULL DEFAULT 0,
    required_stock INTEGER NOT NULL DEFAULT 0,
    recommended_shipment INTEGER NOT NULL DEFAULT 0,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    explanation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stores_store_type ON stores(store_type);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_store_inventory_store_id ON store_inventory(store_id);
CREATE INDEX idx_store_inventory_product_id ON store_inventory(product_id);
CREATE INDEX idx_depot_inventory_product_id ON depot_inventory(product_id);
CREATE INDEX idx_sales_records_store_product_date ON sales_records(store_id, product_id, sale_date);
CREATE INDEX idx_demand_forecasts_store_product_date ON demand_forecasts(store_id, product_id, forecast_date);
CREATE INDEX idx_shipment_recommendations_priority ON shipment_recommendations(priority);
CREATE INDEX idx_shipment_recommendations_status ON shipment_recommendations(status);
CREATE INDEX idx_shipment_recommendations_store_id ON shipment_recommendations(store_id);

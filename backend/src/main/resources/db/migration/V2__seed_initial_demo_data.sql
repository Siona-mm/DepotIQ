INSERT INTO stores (
    store_code,
    name,
    store_type,
    region,
    has_warehouse,
    storage_capacity,
    delivery_lead_time_days,
    preferred_horizon_days
) VALUES
('S001', 'Downtown Small Store', 'SMALL', 'North', false, 500, 2, 3),
('S002', 'Midtown Market', 'MEDIUM', 'North', false, 1200, 2, 7),
('S003', 'Central Large Store', 'LARGE', 'West', true, 3000, 3, 14),
('S004', 'Regional Warehouse Store', 'WAREHOUSE_STORE', 'West', true, 8000, 4, 30)
ON CONFLICT (store_code) DO NOTHING;

INSERT INTO products (
    product_code,
    name,
    category,
    brand,
    supplier_code,
    unit_cost,
    price,
    weight_kg,
    shelf_life_days,
    perishable
) VALUES
('P0001', 'Rice 5kg Bag', 'Groceries', 'Depot Basics', 'SUP001', 4.50, 7.99, 5.000, 365, false),
('P0002', 'Orange Juice 1L', 'Beverages', 'Fresh Valley', 'SUP002', 0.90, 1.99, 1.050, 30, true),
('P0003', 'Laundry Detergent', 'Household', 'CleanPro', 'SUP003', 3.20, 6.49, 2.000, 730, false),
('P0004', 'Winter Jacket', 'Clothing', 'UrbanWear', 'SUP004', 18.00, 49.99, 0.900, 3650, false)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO depot_inventory (
    product_id,
    available_units,
    reserved_units
)
SELECT id, 5000, 0
FROM products
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO store_inventory (
    store_id,
    product_id,
    inventory_level,
    incoming_units
)
SELECT s.id, p.id, 80, 20
FROM stores s
CROSS JOIN products p
ON CONFLICT (store_id, product_id) DO NOTHING;
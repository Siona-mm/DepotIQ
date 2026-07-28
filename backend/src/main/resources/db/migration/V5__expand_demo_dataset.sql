DELETE FROM shipment_recommendations
WHERE demand_forecast_id IN (
    SELECT id
    FROM demand_forecasts
    WHERE model_version = 'seed-v2'
);

DELETE FROM demand_forecasts
WHERE model_version = 'seed-v2';

DELETE FROM shipment_recommendations
WHERE recommendation_date < CURRENT_DATE - 180
  AND status = 'PENDING'
  AND overridden_at IS NULL;

DELETE FROM demand_forecasts df
WHERE df.forecast_date < CURRENT_DATE - 180
  AND NOT EXISTS (
      SELECT 1
      FROM shipment_recommendations sr
      WHERE sr.demand_forecast_id = df.id
  );

DELETE FROM sales_records
WHERE sale_date BETWEEN DATE '2023-10-01' AND DATE '2023-12-29';

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
('S005', 'Riverside Express', 'SMALL', 'South', false, 650, 1, 3),
('S006', 'Eastside Supermarket', 'MEDIUM', 'East', false, 1500, 2, 7),
('S007', 'Lakeside Hypermarket', 'LARGE', 'East', true, 4200, 3, 14),
('S008', 'Southern Distribution Store', 'WAREHOUSE_STORE', 'South', true, 9500, 5, 30),
('S009', 'University Corner Shop', 'SMALL', 'North', false, 450, 1, 3),
('S010', 'Airport Retail Market', 'MEDIUM', 'West', false, 1800, 2, 7)
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
('P0005', 'Whole Milk 1L', 'Dairy', 'Meadow Fresh', 'SUP005', 0.72, 1.39, 1.030, 10, true),
('P0006', 'Greek Yogurt 500g', 'Dairy', 'Meadow Fresh', 'SUP005', 1.20, 2.49, 0.520, 21, true),
('P0007', 'Cheddar Cheese 400g', 'Dairy', 'Farmhouse', 'SUP006', 2.60, 4.99, 0.410, 60, true),
('P0008', 'Bananas 1kg', 'Produce', 'Green Basket', 'SUP007', 0.85, 1.69, 1.000, 7, true),
('P0009', 'Apples 1kg', 'Produce', 'Green Basket', 'SUP007', 1.10, 2.29, 1.000, 21, true),
('P0010', 'Tomatoes 500g', 'Produce', 'Green Basket', 'SUP008', 0.95, 1.99, 0.500, 10, true),
('P0011', 'White Bread 700g', 'Bakery', 'Daily Bake', 'SUP009', 0.80, 1.79, 0.700, 5, true),
('P0012', 'Croissant Pack', 'Bakery', 'Daily Bake', 'SUP009', 1.35, 2.99, 0.360, 4, true),
('P0013', 'Chicken Breast 1kg', 'Meat', 'Prime Farm', 'SUP010', 4.80, 8.99, 1.000, 6, true),
('P0014', 'Frozen Pizza', 'Frozen Food', 'QuickBite', 'SUP011', 1.90, 4.49, 0.450, 180, true),
('P0015', 'French Fries 1kg', 'Frozen Food', 'QuickBite', 'SUP011', 1.45, 3.29, 1.000, 240, true),
('P0016', 'Sparkling Water 6 Pack', 'Beverages', 'ClearSpring', 'SUP012', 1.80, 3.99, 6.300, 365, false),
('P0017', 'Cola 2L', 'Beverages', 'FizzUp', 'SUP013', 0.85, 2.19, 2.100, 270, false),
('P0018', 'Ground Coffee 500g', 'Beverages', 'Roast House', 'SUP014', 3.90, 7.99, 0.520, 365, false),
('P0019', 'Black Tea 100 Bags', 'Beverages', 'Leaf & Co', 'SUP014', 1.75, 4.29, 0.250, 730, false),
('P0020', 'Pasta 500g', 'Groceries', 'Depot Basics', 'SUP001', 0.55, 1.29, 0.500, 730, false),
('P0021', 'Tomato Sauce 500g', 'Groceries', 'Depot Basics', 'SUP001', 0.70, 1.59, 0.520, 540, false),
('P0022', 'Olive Oil 1L', 'Groceries', 'Meditera', 'SUP015', 4.10, 8.49, 0.920, 730, false),
('P0023', 'Breakfast Cereal 750g', 'Groceries', 'Morning Bowl', 'SUP016', 1.85, 3.99, 0.750, 365, false),
('P0024', 'Chocolate Bar 100g', 'Snacks', 'Cocoa Lane', 'SUP017', 0.42, 1.19, 0.100, 365, false),
('P0025', 'Potato Chips 200g', 'Snacks', 'Crunch Time', 'SUP017', 0.75, 1.89, 0.220, 180, false),
('P0026', 'Mixed Nuts 300g', 'Snacks', 'Nut Grove', 'SUP018', 2.30, 4.99, 0.300, 270, false),
('P0027', 'Dishwashing Liquid', 'Household', 'CleanPro', 'SUP003', 1.10, 2.69, 0.780, 730, false),
('P0028', 'Kitchen Towels 4 Pack', 'Household', 'HomeCare', 'SUP019', 1.65, 3.49, 0.650, 1825, false),
('P0029', 'Trash Bags 30 Pack', 'Household', 'HomeCare', 'SUP019', 1.40, 3.19, 0.480, 1825, false),
('P0030', 'Shampoo 400ml', 'Personal Care', 'PureDay', 'SUP020', 1.80, 4.29, 0.430, 1095, false),
('P0031', 'Toothpaste 100ml', 'Personal Care', 'PureDay', 'SUP020', 0.90, 2.39, 0.120, 730, false),
('P0032', 'Hand Soap 500ml', 'Personal Care', 'PureDay', 'SUP020', 1.05, 2.69, 0.540, 730, false),
('P0033', 'Cotton T-Shirt', 'Clothing', 'UrbanWear', 'SUP004', 3.80, 12.99, 0.240, 3650, false),
('P0034', 'Running Shoes', 'Clothing', 'ActiveStep', 'SUP021', 14.50, 39.99, 0.780, 3650, false),
('P0035', 'USB-C Charging Cable', 'Electronics', 'VoltEdge', 'SUP022', 2.40, 8.99, 0.090, 3650, false),
('P0036', 'Wireless Headphones', 'Electronics', 'VoltEdge', 'SUP022', 12.80, 34.99, 0.280, 3650, false),
('P0037', 'LED Desk Lamp', 'Electronics', 'BrightWorks', 'SUP023', 7.50, 19.99, 1.100, 3650, false),
('P0038', 'A4 Notebook', 'Office Supplies', 'PaperPoint', 'SUP024', 0.85, 2.49, 0.350, 3650, false),
('P0039', 'Ballpoint Pens 10 Pack', 'Office Supplies', 'PaperPoint', 'SUP024', 0.95, 2.99, 0.120, 3650, false),
('P0040', 'Storage Box 30L', 'Home Storage', 'OrganizeIt', 'SUP025', 3.60, 9.99, 1.250, 3650, false)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO depot_inventory (
    product_id,
    available_units,
    reserved_units,
    last_updated
)
SELECT
    p.id,
    1200 + MOD(p.id * 173, 4800)::integer,
    25 + MOD(p.id * 37, 350)::integer,
    CURRENT_TIMESTAMP
        - MOD(p.id * 5, 12)::integer * INTERVAL '1 hour'
FROM products p
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO store_inventory (
    store_id,
    product_id,
    inventory_level,
    incoming_units,
    last_updated
)
SELECT
    s.id,
    p.id,
    20 + MOD(s.id * 37 + p.id * 19, 465)::integer,
    MOD(s.id * 11 + p.id * 7, 125)::integer,
    CURRENT_TIMESTAMP
        - MOD(s.id * 3 + p.id, 18)::integer * INTERVAL '1 hour'
FROM stores s
CROSS JOIN products p
ON CONFLICT (store_id, product_id) DO NOTHING;

INSERT INTO sales_records (
    store_id,
    product_id,
    sale_date,
    units_sold,
    price,
    discount,
    promotion,
    weather_condition,
    temperature,
    holiday_promotion,
    seasonality
)
SELECT
    s.id,
    p.id,
    CURRENT_DATE - 90 + day_series.day_offset,
    GREATEST(
        0,
        4
        + MOD(
            s.id * 13
            + p.id * 17
            + day_series.day_offset * 7,
            58
        )::integer
        + CASE
            WHEN p.category IN ('Groceries', 'Beverages', 'Produce', 'Dairy') THEN 18
            WHEN p.category IN ('Bakery', 'Snacks', 'Personal Care') THEN 10
            ELSE 3
          END
        + CASE
            WHEN MOD(s.id + p.id + day_series.day_offset, 13) = 0 THEN 24
            ELSE 0
          END
    ),
    ROUND(
        p.price
        * (
            1
            - (
                MOD(s.id + p.id + day_series.day_offset, 5) * 5
              )::numeric / 100
          ),
        2
    ),
    (MOD(s.id + p.id + day_series.day_offset, 5) * 5)::numeric,
    MOD(s.id + p.id + day_series.day_offset, 13) = 0,
    CASE MOD(s.id * 2 + day_series.day_offset, 4)
        WHEN 0 THEN 'Sunny'
        WHEN 1 THEN 'Cloudy'
        WHEN 2 THEN 'Rainy'
        ELSE 'Snowy'
    END,
    (
        4
        + MOD(s.id * 5 + day_series.day_offset * 3, 27)
    )::numeric,
    day_series.day_offset IN (54, 55, 84, 85, 86),
    CASE
        WHEN EXTRACT(
            MONTH FROM CURRENT_DATE - 90 + day_series.day_offset
        ) IN (12, 1, 2) THEN 'Winter'
        WHEN EXTRACT(
            MONTH FROM CURRENT_DATE - 90 + day_series.day_offset
        ) IN (3, 4, 5) THEN 'Spring'
        WHEN EXTRACT(
            MONTH FROM CURRENT_DATE - 90 + day_series.day_offset
        ) IN (6, 7, 8) THEN 'Summer'
        ELSE 'Autumn'
    END
FROM stores s
CROSS JOIN products p
CROSS JOIN generate_series(0, 89) AS day_series(day_offset)
ON CONFLICT (store_id, product_id, sale_date) DO NOTHING;

WITH recent_sales AS (
    SELECT
        sr.store_id,
        sr.product_id,
        AVG(sr.units_sold)::numeric AS average_daily_sales
    FROM sales_records sr
    WHERE sr.sale_date BETWEEN CURRENT_DATE - 28 AND CURRENT_DATE - 1
    GROUP BY sr.store_id, sr.product_id
)
INSERT INTO demand_forecasts (
    store_id,
    product_id,
    forecast_date,
    horizon_days,
    predicted_demand,
    confidence_lower,
    confidence_upper,
    model_name,
    model_version,
    model_mae
)
SELECT
    s.id,
    p.id,
    CURRENT_DATE - MOD(s.id + p.id, 4)::integer,
    s.preferred_horizon_days,
    ROUND(rs.average_daily_sales * s.preferred_horizon_days, 2),
    ROUND(
        GREATEST(
            0,
            rs.average_daily_sales * s.preferred_horizon_days * 0.88
        ),
        2
    ),
    ROUND(rs.average_daily_sales * s.preferred_horizon_days * 1.12, 2),
    'DepotIQ deterministic demo model',
    'seed-v2',
    ROUND(5 + MOD(s.id * 7 + p.id * 3, 160)::numeric / 10, 4)
FROM recent_sales rs
JOIN stores s ON s.id = rs.store_id
JOIN products p ON p.id = rs.product_id
ON CONFLICT (
    store_id,
    product_id,
    forecast_date,
    horizon_days
) DO NOTHING;

WITH prepared_recommendations AS (
    SELECT
        df.id AS demand_forecast_id,
        df.store_id,
        df.product_id,
        df.forecast_date,
        df.horizon_days,
        df.predicted_demand,
        df.confidence_lower,
        df.confidence_upper,
        si.inventory_level,
        si.incoming_units,
        CEIL(df.predicted_demand * 0.15)::integer AS safety_stock,
        CEIL(df.predicted_demand * 1.15)::integer AS required_stock,
        GREATEST(
            0,
            CEIL(df.predicted_demand * 1.15)::integer
                - si.inventory_level
                - si.incoming_units
        ) AS recommended_shipment
    FROM demand_forecasts df
    JOIN store_inventory si
      ON si.store_id = df.store_id
     AND si.product_id = df.product_id
    WHERE df.model_version = 'seed-v2'
)
INSERT INTO shipment_recommendations (
    store_id,
    product_id,
    demand_forecast_id,
    recommendation_date,
    horizon_days,
    predicted_demand,
    confidence_lower,
    confidence_upper,
    current_inventory,
    incoming_units,
    safety_stock,
    required_stock,
    recommended_shipment,
    priority,
    status,
    explanation
)
SELECT
    pr.store_id,
    pr.product_id,
    pr.demand_forecast_id,
    pr.forecast_date,
    pr.horizon_days,
    pr.predicted_demand,
    pr.confidence_lower,
    pr.confidence_upper,
    pr.inventory_level,
    pr.incoming_units,
    pr.safety_stock,
    pr.required_stock,
    pr.recommended_shipment,
    CASE
        WHEN pr.recommended_shipment >= 1000 THEN 'URGENT'
        WHEN pr.recommended_shipment >= 500 THEN 'HIGH'
        WHEN pr.recommended_shipment >= 100 THEN 'NORMAL'
        ELSE 'LOW'
    END,
    'PENDING',
    'Demo recommendation based on historical sales, store horizon, current stock, incoming units, and 15% safety stock.'
FROM prepared_recommendations pr
ON CONFLICT (
    store_id,
    product_id,
    recommendation_date,
    horizon_days
) DO NOTHING;

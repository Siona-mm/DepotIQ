CREATE SEQUENCE store_code_seq START WITH 1;
CREATE SEQUENCE product_code_seq START WITH 1;
CREATE SEQUENCE shipment_number_seq START WITH 1;

SELECT setval(
    'store_code_seq',
    COALESCE((
        SELECT MAX(SUBSTRING(store_code FROM 2)::BIGINT) + 1
        FROM stores
        WHERE store_code ~ '^S[0-9]+$'
    ), 1),
    FALSE
);

SELECT setval(
    'product_code_seq',
    COALESCE((
        SELECT MAX(SUBSTRING(product_code FROM 2)::BIGINT) + 1
        FROM products
        WHERE product_code ~ '^P[0-9]+$'
    ), 1),
    FALSE
);

SELECT setval(
    'shipment_number_seq',
    COALESCE((
        SELECT MAX(SUBSTRING(shipment_number FROM '([0-9]+)$')::BIGINT) + 1
        FROM shipments
        WHERE shipment_number ~ '^SHP-[0-9]{4}-[0-9]+$'
    ), 1),
    FALSE
);

ALTER TABLE products
    ADD COLUMN external_sku VARCHAR(100);

CREATE UNIQUE INDEX uk_products_external_sku
    ON products (LOWER(external_sku))
    WHERE external_sku IS NOT NULL;

-- Preserve store primary keys and all foreign-key relationships. Only padding changes.
CREATE TEMP TABLE normalized_store_codes ON COMMIT DROP AS
SELECT id, store_code AS old_code,
       'S' || LPAD(digits, GREATEST(3, LENGTH(digits)), '0') AS new_code
FROM (
    SELECT id, store_code,
           COALESCE(NULLIF(LTRIM(SUBSTRING(store_code FROM 2), '0'), ''), '0') AS digits
    FROM stores WHERE store_code ~* '^S[0-9]+$'
) numeric_codes;

-- Never merge distinct stores that happen to share the same numeric code.
DO $$
BEGIN
    IF EXISTS (SELECT new_code FROM normalized_store_codes GROUP BY new_code HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Store codes have conflicting numeric identities; resolve duplicates before normalizing padding';
    END IF;
END $$;

UPDATE stores s SET store_code = n.new_code, updated_at = CURRENT_TIMESTAMP
FROM normalized_store_codes n WHERE s.id = n.id AND s.store_code <> n.new_code;

-- Legacy CSV imports inserted their own codes without advancing these sequences.
-- Never move a sequence backwards, including numbers consumed by rolled-back inserts.
SELECT setval('store_code_seq', GREATEST(
    COALESCE((SELECT MAX(SUBSTRING(store_code FROM 2)::BIGINT) + 1 FROM stores WHERE store_code ~ '^S[0-9]+$'), 1),
    (SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM store_code_seq)
), FALSE);

SELECT setval('product_code_seq', GREATEST(
    COALESCE((SELECT MAX(SUBSTRING(product_code FROM 2)::BIGINT) + 1 FROM products WHERE product_code ~ '^P[0-9]+$'), 1),
    (SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM product_code_seq)
), FALSE);

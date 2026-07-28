ALTER TABLE shipment_recommendations
ADD CONSTRAINT uq_recommendation_store_product_date_horizon
UNIQUE (store_id, product_id, recommendation_date, horizon_days);

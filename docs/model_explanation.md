# DepotIQ Model Explanation

The first model will use supervised regression to predict future product demand.

Planned baseline:

- average historical sales
- Linear Regression
- Random Forest Regressor

## Notebook 11: 14-Day Large-Store Demand Model

`11_train_14_day_large_store_model.py` trains a leakage-safe model for large stores
with stockrooms.

The model uses:

- Target: `DemandNext14Days`
- Store scope: `Large` and `Warehouse Store`
- Stockroom requirement: `Has Warehouse = 1`
- Planning requirement: `Preferred Horizon Days >= 14`
- Test method: unseen future dates
- Baseline: average `DemandNext14Days` by store-product pair

The script excludes leakage-prone operational and target columns:

- `Demand Forecast`
- `Units Ordered`
- `Incoming Units`
- `True Demand`
- `Stockout Units`
- `DemandNext3Days`
- `DemandNext7Days`
- `DemandNext14Days`
- `DemandNext30Days`

The model features include store/product/category fields, warehouse capacity,
lead time, price, cost, promotion, weather, seasonality, date features, lagged
sales, and rolling sales averages.

The training split reserves the latest 60 days for testing. The training cutoff
is purged by 14 days before the test period so that no training label includes
sales from the unseen future test dates.

The script records:

- baseline metrics
- model metrics
- MAE improvement over baseline
- largest under-predictions
- largest over-predictions
- mean absolute error by category
- mean absolute error by store type

Outputs are saved to:

```text
ml-service/models/large_store_14_day_demand_model.joblib
ml-service/models/large_store_14_day_metrics.csv
ml-service/models/large_store_14_day_prediction_errors.csv
```

## Notebook 12: 7-Day Limited-Storage Demand Model

`12_train_7_day_demand_model.py` trains a leakage-safe weekly model for stores
with limited storage.

The model uses:

- Target: `DemandNext7Days`
- Store scope: stores without a warehouse
- Planning requirement: `Preferred Horizon Days <= 7`
- Test method: unseen future dates
- Baseline: trailing 7-day sales for each store-product pair

The script excludes leakage-prone operational and target columns:

- `Demand Forecast`
- `Units Ordered`
- `Incoming Units`
- `True Demand`
- `Stockout Units`
- `DemandNext3Days`
- `DemandNext7Days`
- `DemandNext14Days`
- `DemandNext30Days`

The model features include store/product/category fields, storage capacity,
lead time, price, cost, promotion, weather, seasonality, date features, lagged
sales, rolling sales averages, and the trailing 7-day sales sum.

The training split reserves the latest 60 days for testing. The training cutoff
is purged by 7 days before the test period so that no training label includes
sales from the unseen future test dates.

The latest recorded run produced:

- trailing 7-day baseline MAE: 20.70
- model MAE: 17.14
- MAE improvement over baseline: 17.2%
- training time: 2.64 seconds

Outputs are saved to:

```text
ml-service/models/demand_model_7_day.joblib
ml-service/models/demand_model_7_day_metrics.csv
ml-service/models/demand_model_7_day_prediction_errors.csv
```

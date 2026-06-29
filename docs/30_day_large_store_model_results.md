# 30-Day Large-Store Demand Model Results

This document records the results from `13_train_30_day_demand_model.py`.

## Scope

The model trains only on large stores with stockrooms, restricted to the
longest planning horizon:

- `Store Type` is `Large` or `Warehouse Store`
- `Has Warehouse = 1`
- `Preferred Horizon Days >= 30`

Target:

- `DemandNext30Days`

## Data Split

The model is evaluated on unseen future dates.

| Split detail | Value |
|---|---:|
| Rows with target | 92,500 |
| Eligible rows | 18,500 |
| Rows after historical features | 17,000 |
| Training rows | 12,500 |
| Testing rows | 3,000 |
| Training cutoff | Before 2023-10-08 |
| Test period | 2023-11-07 to 2024-01-05 |

The training cutoff is purged by 30 days before the test period so training
labels do not include future sales from the test window.

## Metrics

| Model | Training Time | MAE | RMSE | R2 | Normalized MAE |
|---|---:|---:|---:|---:|---:|
| Store-product 30-day baseline | 0.00s | 149.93 | 185.36 | 0.918 | 17.51% |
| Hist Gradient Boosting | 5.18s | 105.73 | 151.34 | 0.945 | 12.35% |

The trained model improves MAE by 29.5% compared with the store-product
baseline.

## Eligible Population

Only two stores and 25 products meet the 30-day eligibility criteria in the
synthetic dataset:

- Stores: `S004`, `S008`
- Store types: `Warehouse Store`

Because the eligible population is so small, results are concentrated on a
handful of high-volume store-product pairs rather than spread across a broad
store network.

## Common Prediction Errors

Largest under-predictions were concentrated around beverage demand for store
`S008` and product `P0002` in mid-to-late December, where actual 30-day
demand ran 590-620 units above the model's prediction.

Largest over-predictions were concentrated around grocery demand for store
`S008` and product `P0021`, and beverage demand for store `S004` and product
`P0012`, where the model overshot actual demand by roughly 440-465 units.

Mean absolute error by category:

| Category | MAE |
|---|---:|
| Beverages | 245.06 |
| Groceries | 192.23 |
| Personal Care | 118.14 |
| Clothing | 99.06 |
| Household | 90.75 |
| Office Supplies | 84.18 |
| Sports | 64.03 |
| Electronics | 60.67 |
| Toys | 35.26 |
| Furniture | 22.52 |

Mean absolute error by store type:

| Store Type | MAE |
|---|---:|
| Warehouse Store | 105.73 |

All eligible rows belong to `Warehouse Store` types, so this is the only
store-type breakdown available at this horizon.

## Saved Artifacts

Running the script saves local artifacts to:

```text
ml-service/models/demand_model_30_day.joblib
ml-service/models/demand_model_30_day_metrics.csv
ml-service/models/demand_model_30_day_prediction_errors.csv
```

These files are intentionally ignored by Git because model and generated data
artifacts should remain local.

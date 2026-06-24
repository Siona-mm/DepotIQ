# 14-Day Large-Store Demand Model Results

This document records the results from `11_train_14_day_large_store_model.py`.

## Scope

The model trains only on large stores with stockrooms:

- `Store Type` is `Large` or `Warehouse Store`
- `Has Warehouse = 1`
- `Preferred Horizon Days >= 14`

Target:

- `DemandNext14Days`

## Data Split

The model is evaluated on unseen future dates.

| Split detail | Value |
|---|---:|
| Rows with target | 92,500 |
| Eligible rows | 37,000 |
| Rows after historical features | 34,000 |
| Training rows | 26,600 |
| Testing rows | 6,000 |
| Training cutoff | Before 2023-10-24 |
| Test period | 2023-11-07 to 2024-01-05 |

The training cutoff is purged by 14 days before the test period so training
labels do not include future sales from the test window.

## Metrics

| Model | MAE | RMSE | R2 | Normalized MAE |
|---|---:|---:|---:|---:|
| Store-product baseline | 62.60 | 80.83 | 0.916 | 16.95% |
| Hist Gradient Boosting | 41.58 | 61.04 | 0.952 | 11.26% |

The trained model improves MAE by 33.6% compared with the store-product
baseline.

## Common Prediction Errors

Largest under-predictions were concentrated around grocery demand for store
`S008` and product `P0021` in early December.

Largest over-predictions were concentrated around beverage demand for store
`S004` and product `P0012` around December 24-28.

Mean absolute error by category:

| Category | MAE |
|---|---:|
| Beverages | 78.72 |
| Groceries | 73.44 |
| Personal Care | 51.34 |
| Household | 41.07 |
| Electronics | 36.77 |
| Clothing | 34.68 |
| Office Supplies | 32.74 |
| Sports | 29.66 |
| Toys | 14.36 |
| Furniture | 10.14 |

Mean absolute error by store type:

| Store Type | MAE |
|---|---:|
| Warehouse Store | 46.70 |
| Large | 36.47 |

## Saved Artifacts

Running the script saves local artifacts to:

```text
ml-service/models/large_store_14_day_demand_model.joblib
ml-service/models/large_store_14_day_metrics.csv
ml-service/models/large_store_14_day_prediction_errors.csv
```

These files are intentionally ignored by Git because model and generated data
artifacts should remain local.

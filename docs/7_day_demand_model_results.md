# 7-Day Limited-Storage Demand Model Results

This document records the results from `12_train_7_day_demand_model.py`.

## Scope

The model trains only on limited-storage stores:

- `Has Warehouse = 0`
- `Preferred Horizon Days <= 7`
- In the synthetic dataset, this includes `Small` and `Medium` stores

Target:

- `DemandNext7Days`

## Data Split

The model is evaluated on unseen future dates.

| Split detail | Value |
|---|---:|
| Rows with target | 92,500 |
| Eligible rows | 55,500 |
| Rows after historical features | 51,000 |
| Training rows | 40,950 |
| Testing rows | 9,000 |
| Training cutoff | Before 2023-10-31 |
| Test period | 2023-11-07 to 2024-01-05 |

The training cutoff is purged by 7 days before the test period so training
labels do not include future sales from the test window.

## Metrics

| Model | Training Time | MAE | RMSE | R2 | Normalized MAE |
|---|---:|---:|---:|---:|---:|
| Trailing 7-day sales baseline | 0.00s | 20.70 | 30.12 | 0.899 | 17.77% |
| Hist Gradient Boosting | 2.64s | 17.14 | 25.52 | 0.928 | 14.71% |

The trained model improves MAE by 17.2% compared with the trailing 7-day sales
baseline.

## Common Prediction Errors

Largest under-predictions were concentrated around beverage demand for store
`S010` and products `P0002` and `P0012` in late November.

Largest over-predictions were concentrated around grocery and beverage demand
for stores `S001`, `S002`, `S005`, and `S010`.

Mean absolute error by category:

| Category | MAE |
|---|---:|
| Beverages | 30.50 |
| Groceries | 29.17 |
| Household | 22.89 |
| Personal Care | 18.29 |
| Office Supplies | 14.96 |
| Electronics | 14.31 |
| Sports | 13.60 |
| Clothing | 12.96 |
| Toys | 6.74 |
| Furniture | 4.87 |

Mean absolute error by store type:

| Store Type | MAE |
|---|---:|
| Medium | 19.71 |
| Small | 14.57 |

## Saved Artifacts

Running the script saves local artifacts to:

```text
ml-service/models/demand_model_7_day.joblib
ml-service/models/demand_model_7_day_metrics.csv
ml-service/models/demand_model_7_day_prediction_errors.csv
```

These files are intentionally ignored by Git because model and generated data
artifacts should remain local.

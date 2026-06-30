# Multi-Horizon Model Comparison Report

## Purpose

DepotIQ uses multiple demand forecasting models because different stores need different replenishment timelines.

Small stores usually have limited storage, so they need shorter and more frequent shipment planning. Larger stores and warehouse stores can hold more inventory, so they can plan further ahead.

Instead of using one model for every store, DepotIQ uses a model that matches the store's storage capacity and planning horizon.

## Horizon Strategy

| Store Type | Forecast Horizon | Target Column | Model Purpose |
|---|---:|---|---|
| Small store | 3 days | DemandNext3Days | Short-term replenishment for stores with very limited storage |
| Medium store | 7 days | DemandNext7Days | Weekly planning for stores with limited storage |
| Large store with stockroom | 14 days | DemandNext14Days | Longer planning for stores that can hold more inventory |
| Warehouse store | 30 days | DemandNext30Days | Monthly planning for stores with warehouse capacity |

## Model Comparison

| Horizon | Intended Store Type | Target Column | Baseline MAE | Model MAE | RMSE | R² | Normalized MAE | Improvement Over Baseline | Saved Model File |
|---|---|---|---:|---:|---:|---:|---:|---:|---|
| 3 days | Small stores | DemandNext3Days | 13.95 | 11.85 | 17.91 | 0.886 | 19.32% | 15.1% | ml-service/models/synthetic_honest_3_day_model.joblib |
| 7 days | Small and medium limited-storage stores | DemandNext7Days | 20.70 | 17.14 | 25.52 | 0.928 | 14.71% | 17.2% | ml-service/models/demand_model_7_day.joblib |
| 14 days | Large stores with stockrooms | DemandNext14Days | 62.60 | 41.58 | 61.04 | 0.952 | 11.26% | 33.6% | ml-service/models/large_store_14_day_demand_model.joblib |
| 30 days | Warehouse stores | DemandNext30Days | 149.93 | 105.73 | 151.34 | 0.945 | 12.35% | 29.5% | ml-service/models/demand_model_30_day.joblib |

## Metric Meaning

### MAE

MAE means Mean Absolute Error.

It shows how many units the model is wrong by on average.

Example:

If the MAE is 17.14, the model is usually wrong by about 17 units for that prediction horizon.

Lower MAE is better.

### RMSE

RMSE means Root Mean Squared Error.

It also measures prediction error, but it punishes large mistakes more than MAE.

Lower RMSE is better.

### R²

R² shows how much of the demand pattern the model explains.

A score closer to 1 is better.

Example:

An R² of 0.928 means the model explains about 92.8% of the variation in demand.

### Normalized MAE

Normalized MAE shows the average error as a percentage of average demand.

This makes it easier to compare models with different horizons.

Example:

A 30-day model will naturally have bigger unit errors than a 3-day model because it predicts a longer time period. Normalized MAE helps compare them more fairly.

## Leakage Prevention

All final horizon models were trained using leakage-safe features.

The models exclude columns that would give away future information or target-related information, including:

- Demand Forecast
- Units Ordered
- Incoming Units
- True Demand
- Stockout Units
- DemandNext3Days
- DemandNext7Days
- DemandNext14Days
- DemandNext30Days

The target column is used only as the label during training, not as an input feature.

## Time-Based Testing

The models were tested on future dates that were not used during training.

A gap was left between the training period and test period for each model. This prevents a training target from accidentally including sales from the test period.

This makes the evaluation more realistic because the model is tested like it would be used in the real application: trained on past data and tested on future demand.

## Conclusion

The multi-horizon forecasting approach works well for DepotIQ.

All four models improved over their baseline comparisons:

- The 3-day model improved MAE by 15.1%.
- The 7-day model improved MAE by 17.2%.
- The 14-day model improved MAE by 33.6%.
- The 30-day model improved MAE by 29.5%.

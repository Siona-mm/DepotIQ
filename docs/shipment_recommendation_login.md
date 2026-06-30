# Shipment Recommendation Logic

## Purpose

The shipment recommendation logic converts DepotIQ demand forecasts into suggested shipment quantities.

The forecasting models predict how much demand a store will have over a specific time horizon. The recommendation logic then uses that prediction, along with inventory and store capacity information, to decide how much stock the depot should send.

This logic is used after the demand forecasting model has already made a prediction.

## Model Selection Rule

DepotIQ uses different forecasting models depending on the store type and storage capacity.

| Store Type | Forecast Horizon | Model Used |
|---|---:|---|
| Small store | 3 days | 3-day demand model |
| Medium store | 7 days | 7-day demand model |
| Large store with stockroom | 14 days | 14-day demand model |
| Warehouse store | 30 days | 30-day demand model |

This allows DepotIQ to match the prediction period to how much inventory the store can realistically hold.

Small stores need shorter planning windows because they have less storage space. Warehouse stores can plan further ahead because they can hold more stock.

## Required Inputs

The recommendation logic needs the following values:

| Input | Meaning |
|---|---|
| Store ID | The store receiving stock |
| Product ID | The product being forecasted |
| Store Type | Used to select the correct forecast horizon |
| Predicted Demand | Output from the selected ML model |
| Horizon Days | Number of days the model predicts |
| Current Inventory | Current stock available at the store |
| Incoming Units | Stock already on the way to the store |
| Storage Capacity | Maximum stock the store can hold |
| Delivery Lead Time | Number of days it takes for a shipment to arrive |
| Safety Stock Ratio | Extra stock buffer, currently 15% |

## Shipment Formula

The basic formula is:

```text
Safety Stock = Predicted Demand × 15%
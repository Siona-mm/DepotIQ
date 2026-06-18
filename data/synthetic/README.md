# DepotIQ Synthetic Retail Dataset

`depotiq_synthetic_retail.csv` is generated data created for the DepotIQ educational prototype. It is not a record of real stores, customers, products, or transactions.

Generate it from the repository root with:

```bash
cd ml-service
source .venv/bin/activate
python scripts/generate_synthetic_retail_data.py
```

The generator uses a fixed random seed and models:

- ten stores with different sizes, warehouse access, capacities, lead times, and planning horizons;
- twenty-five products across ten categories;
- realistic product names, brands, suppliers, unit costs, retail prices, weights, shelf lives, and perishability;
- weekly and annual seasonality;
- promotions, discounts, price elasticity, holidays, and weather;
- product and store growth patterns;
- inventory, incoming stock, replenishment, and lost demand from stockouts;
- random demand variation that prevents unrealistically perfect forecasts.

Use `Units Sold` as the observed demand target. `True Demand` includes unmet demand when inventory was unavailable. Do not use `Demand Forecast`, `Units Ordered`, `Incoming Units`, `True Demand`, or `Stockout Units` as predictors for an honest `Units Sold` model unless their availability at prediction time is explicitly justified.

Generated CSV files should remain excluded from Git. Commit this README and the generator script so every team member can reproduce the same dataset locally.

The defaults can be changed when generating:

```bash
python scripts/generate_synthetic_retail_data.py --stores 30 --products 150
```

Larger values increase generation and model-training time significantly. The default 10 stores, 25 products, and 400 days produce exactly 100,000 daily records.

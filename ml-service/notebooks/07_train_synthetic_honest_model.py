"""Train and validate a leakage-safe daily demand model on synthetic data."""

from pathlib import Path
from time import perf_counter

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder


DATA_PATH = Path(__file__).resolve().parents[2] / "data" / "synthetic" / "depotiq_synthetic_retail.csv"
MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "synthetic_honest_daily_model.joblib"
TEST_DAYS = 60


def add_historical_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.sort_values(["Store ID", "Product ID", "Date"]).copy()
    sales = df.groupby(["Store ID", "Product ID"])["Units Sold"]

    for lag in (1, 7, 14, 30):
        df[f"SalesLag{lag}"] = sales.shift(lag)

    for window in (7, 14, 30):
        df[f"SalesRolling{window}"] = sales.transform(
            lambda values: values.shift(1).rolling(window).mean()
        )

    df["DayOfWeek"] = df["Date"].dt.dayofweek
    df["Month"] = df["Date"].dt.month
    df["DayOfYearSin"] = np.sin(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["DayOfYearCos"] = np.cos(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["IsWeekend"] = (df["DayOfWeek"] >= 5).astype(int)
    return df.dropna().reset_index(drop=True)


def print_metrics(name: str, actual: pd.Series, predicted: np.ndarray) -> dict[str, float]:
    metrics = {
        "mae": mean_absolute_error(actual, predicted),
        "rmse": float(np.sqrt(mean_squared_error(actual, predicted))),
        "r2": r2_score(actual, predicted),
    }
    print(f"\n{name}")
    print(f"MAE: {metrics['mae']:.2f}")
    print(f"RMSE: {metrics['rmse']:.2f}")
    print(f"R2: {metrics['r2']:.3f}")
    return metrics


def main() -> None:
    print("Loading synthetic DepotIQ data...")
    df = pd.read_csv(DATA_PATH, parse_dates=["Date"])
    df = add_historical_features(df)

    test_start = df["Date"].max() - pd.Timedelta(days=TEST_DAYS - 1)
    train = df[df["Date"] < test_start].copy()
    test = df[df["Date"] >= test_start].copy()

    categorical_features = [
        "Store ID",
        "Product ID",
        "Category",
        "Region",
        "Store Type",
        "Brand",
        "Supplier ID",
        "Weather Condition",
        "Seasonality",
    ]
    numeric_features = [
        "Has Warehouse",
        "Storage Capacity",
        "Delivery Lead Time",
        "Preferred Horizon Days",
        "Inventory Level",
        "Price",
        "Unit Cost",
        "Weight Kg",
        "Shelf Life Days",
        "Perishable",
        "Discount",
        "Temperature",
        "Holiday/Promotion",
        "Promotion",
        "Competitor Pricing",
        "SalesLag1",
        "SalesLag7",
        "SalesLag14",
        "SalesLag30",
        "SalesRolling7",
        "SalesRolling14",
        "SalesRolling30",
        "DayOfWeek",
        "Month",
        "DayOfYearSin",
        "DayOfYearCos",
        "IsWeekend",
    ]
    features = categorical_features + numeric_features

    excluded_leakage_features = [
        "Demand Forecast",
        "Units Ordered",
        "Incoming Units",
        "True Demand",
        "Stockout Units",
    ]

    print(f"Rows after lag creation: {len(df):,}")
    print(f"Training rows: {len(train):,}")
    print(f"Testing rows: {len(test):,}")
    print(f"Test period: {test['Date'].min().date()} to {test['Date'].max().date()}")
    print("Excluded leakage features:", ", ".join(excluded_leakage_features))

    global_mean = train["Units Sold"].mean()
    series_means = train.groupby(["Store ID", "Product ID"])["Units Sold"].mean()
    baseline = np.array(
        [series_means.get((store, product), global_mean) for store, product in zip(test["Store ID"], test["Product ID"])]
    )
    baseline_metrics = print_metrics("Store-product average baseline", test["Units Sold"], baseline)

    preprocessor = ColumnTransformer(
        transformers=[
            ("categories", OneHotEncoder(handle_unknown="ignore", sparse_output=False), categorical_features),
            ("numbers", "passthrough", numeric_features),
        ]
    )
    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            (
                "model",
                HistGradientBoostingRegressor(
                    learning_rate=0.07,
                    max_iter=250,
                    max_leaf_nodes=31,
                    min_samples_leaf=30,
                    l2_regularization=1.0,
                    random_state=42,
                ),
            ),
        ]
    )

    print("\nTraining honest daily model...")
    started = perf_counter()
    pipeline.fit(train[features], train["Units Sold"])
    training_seconds = perf_counter() - started
    predictions = np.maximum(0, pipeline.predict(test[features]))
    model_metrics = print_metrics("Hist Gradient Boosting", test["Units Sold"], predictions)

    improvement = (baseline_metrics["mae"] - model_metrics["mae"]) / baseline_metrics["mae"] * 100
    print(f"MAE improvement over baseline: {improvement:.1f}%")
    print(f"Training time: {training_seconds:.1f} seconds")

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "pipeline": pipeline,
            "features": features,
            "target": "Units Sold",
            "test_start": str(test_start.date()),
            "metrics": model_metrics,
            "data_label": "synthetic",
        },
        MODEL_PATH,
    )
    print(f"Model saved to: {MODEL_PATH}")
    print("\nNote: this is a rolling one-day forecast using only previous sales values.")


if __name__ == "__main__":
    main()

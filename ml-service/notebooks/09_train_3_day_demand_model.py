"""Train and validate a leakage-safe three-day demand model."""

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


DATA_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "processed"
    / "depotiq_multiday_targets.csv"
)
MODEL_PATH = (
    Path(__file__).resolve().parents[1]
    / "models"
    / "synthetic_honest_3_day_model.joblib"
)
TARGET = "DemandNext3Days"
HORIZON_DAYS = 3
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


def evaluate(name: str, actual: pd.Series, predicted: np.ndarray) -> dict[str, float]:
    mae = mean_absolute_error(actual, predicted)
    metrics = {
        "mae": mae,
        "rmse": float(np.sqrt(mean_squared_error(actual, predicted))),
        "r2": r2_score(actual, predicted),
        "normalized_mae_percent": float(mae / actual.mean() * 100),
    }

    print(f"\n{name}")
    print(f"MAE: {metrics['mae']:.2f}")
    print(f"RMSE: {metrics['rmse']:.2f}")
    print(f"R2: {metrics['r2']:.3f}")
    print(f"Normalized MAE: {metrics['normalized_mae_percent']:.2f}%")
    return metrics


def main() -> None:
    print("Loading multi-day target data...")
    df = pd.read_csv(DATA_PATH, parse_dates=["Date"])
    df = add_historical_features(df)

    test_start = df["Date"].max() - pd.Timedelta(days=TEST_DAYS - 1)

    # A training target at date t contains sales through t + 3. Purging the
    # boundary prevents any training label from containing a test-period day.
    train_cutoff = test_start - pd.Timedelta(days=HORIZON_DAYS)
    train = df[df["Date"] < train_cutoff].copy()
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

    excluded_features = [
        "Demand Forecast",
        "Units Ordered",
        "Incoming Units",
        "True Demand",
        "Stockout Units",
        "DemandNext3Days",
        "DemandNext7Days",
        "DemandNext14Days",
        "DemandNext30Days",
    ]

    print(f"Rows after lag creation: {len(df):,}")
    print(f"Training rows: {len(train):,}")
    print(f"Testing rows: {len(test):,}")
    print(f"Training ends before: {train_cutoff.date()}")
    print(f"Test period: {test['Date'].min().date()} to {test['Date'].max().date()}")
    print("Target:", TARGET)
    print("Excluded features:", ", ".join(excluded_features))

    global_mean = train[TARGET].mean()
    series_means = train.groupby(["Store ID", "Product ID"])[TARGET].mean()
    baseline = np.array(
        [
            series_means.get((store, product), global_mean)
            for store, product in zip(test["Store ID"], test["Product ID"])
        ]
    )
    baseline_metrics = evaluate("Store-product 3-day baseline", test[TARGET], baseline)

    preprocessor = ColumnTransformer(
        transformers=[
            (
                "categories",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                categorical_features,
            ),
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

    print("\nTraining honest 3-day demand model...")
    started = perf_counter()
    pipeline.fit(train[features], train[TARGET])
    training_seconds = perf_counter() - started

    predictions = np.maximum(0, pipeline.predict(test[features]))
    model_metrics = evaluate("Hist Gradient Boosting 3-day model", test[TARGET], predictions)

    improvement = (
        (baseline_metrics["mae"] - model_metrics["mae"])
        / baseline_metrics["mae"]
        * 100
    )
    print(f"MAE improvement over baseline: {improvement:.1f}%")
    print(f"Training time: {training_seconds:.1f} seconds")

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "pipeline": pipeline,
            "features": features,
            "target": TARGET,
            "horizon_days": HORIZON_DAYS,
            "test_start": str(test_start.date()),
            "metrics": model_metrics,
            "baseline_metrics": baseline_metrics,
            "data_label": "synthetic",
        },
        MODEL_PATH,
    )

    print(f"Model saved to: {MODEL_PATH}")
    print("\nThe model predicts total units sold during the next 3 days.")


if __name__ == "__main__":
    main()

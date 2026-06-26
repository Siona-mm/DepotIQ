"""Train a leakage-safe 7-day demand model for limited-storage stores."""

from pathlib import Path
from time import perf_counter

import joblib
import numpy as np
import pandas as pd
from pandas import DataFrame, Series
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
    / "demand_model_7_day.joblib"
)
METRICS_PATH = (
    Path(__file__).resolve().parents[1]
    / "models"
    / "demand_model_7_day_metrics.csv"
)
ERRORS_PATH = (
    Path(__file__).resolve().parents[1]
    / "models"
    / "demand_model_7_day_prediction_errors.csv"
)
TARGET = "DemandNext7Days"
HORIZON_DAYS = 7
TEST_DAYS = 60
GROUP_COLUMNS = ["Store ID", "Product ID"]

CATEGORICAL_FEATURES = [
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
NUMERIC_FEATURES = [
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
    "SalesTrailing7Sum",
    "DayOfWeek",
    "Month",
    "DayOfYearSin",
    "DayOfYearCos",
    "IsWeekend",
]
FEATURES = CATEGORICAL_FEATURES + NUMERIC_FEATURES
EXCLUDED_FEATURES = [
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


def load_target_data() -> DataFrame:
    if not DATA_PATH.exists():
        raise FileNotFoundError(
            f"Missing processed dataset at {DATA_PATH}. "
            "Run 08_create_multiday_targets.py before training."
        )

    df = pd.read_csv(DATA_PATH, parse_dates=["Date"])
    if TARGET not in df.columns:
        raise ValueError(
            f"Missing target column {TARGET}. "
            "Run 08_create_multiday_targets.py to create multi-day targets."
        )

    complete = df.dropna(subset=[TARGET]).copy()
    complete[TARGET] = complete[TARGET].astype(float)
    return DataFrame(complete)


def filter_limited_storage_stores(df: DataFrame) -> DataFrame:
    required_columns = [
        "Has Warehouse",
        "Preferred Horizon Days",
        "Storage Capacity",
        "Store Type",
    ]
    missing = [column for column in required_columns if column not in df.columns]
    if missing:
        raise ValueError(f"Missing required store columns: {', '.join(missing)}")

    eligible = df[
        (df["Has Warehouse"] == 0)
        & (df["Preferred Horizon Days"] <= HORIZON_DAYS)
    ].copy()

    if eligible.empty:
        raise ValueError("No limited-storage stores were found.")

    return DataFrame(eligible.reset_index(drop=True))


def add_historical_features(df: DataFrame) -> DataFrame:
    df = df.sort_values([*GROUP_COLUMNS, "Date"]).copy()
    sales = df.groupby(GROUP_COLUMNS, sort=False)["Units Sold"]

    for lag in (1, 7, 14, 30):
        df[f"SalesLag{lag}"] = sales.shift(lag)

    for window in (7, 14, 30):
        df[f"SalesRolling{window}"] = sales.transform(
            lambda values: values.shift(1).rolling(window).mean()
        )

    df["SalesTrailing7Sum"] = sales.transform(
        lambda values: values.shift(1).rolling(HORIZON_DAYS).sum()
    )
    df["DayOfWeek"] = df["Date"].dt.dayofweek
    df["Month"] = df["Date"].dt.month
    df["DayOfYearSin"] = np.sin(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["DayOfYearCos"] = np.cos(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["IsWeekend"] = (df["DayOfWeek"] >= 5).astype(int)

    return DataFrame(df.dropna(subset=FEATURES + [TARGET]).reset_index(drop=True))


def validate_feature_columns(df: DataFrame) -> None:
    missing = [column for column in FEATURES if column not in df.columns]
    if missing:
        raise ValueError(f"Missing feature columns: {', '.join(missing)}")

    leaked = [column for column in EXCLUDED_FEATURES if column in FEATURES]
    target_like = [column for column in FEATURES if column.startswith("DemandNext")]
    if leaked or target_like:
        leakage_columns = sorted(set(leaked + target_like))
        raise ValueError(
            "Leakage columns included as features: "
            + ", ".join(leakage_columns)
        )


def split_future_dates(df: DataFrame) -> tuple[DataFrame, DataFrame, pd.Timestamp, pd.Timestamp]:
    test_start = df["Date"].max() - pd.Timedelta(days=TEST_DAYS - 1)
    train_cutoff = test_start - pd.Timedelta(days=HORIZON_DAYS)
    train = df[df["Date"] < train_cutoff].copy()
    test = df[df["Date"] >= test_start].copy()

    if train.empty or test.empty:
        raise ValueError("Future-date split produced an empty train or test set.")

    return train, test, test_start, train_cutoff


def evaluate(name: str, actual: Series, predicted: np.ndarray) -> dict[str, float]:
    mae = mean_absolute_error(actual, predicted)
    metrics = {
        "mae": float(mae),
        "rmse": float(np.sqrt(mean_squared_error(actual, predicted))),
        "r2": float(r2_score(actual, predicted)),
        "normalized_mae_percent": float(mae / actual.mean() * 100),
    }

    print(f"\n{name}")
    print(f"MAE: {metrics['mae']:.2f}")
    print(f"RMSE: {metrics['rmse']:.2f}")
    print(f"R2: {metrics['r2']:.3f}")
    print(f"Normalized MAE: {metrics['normalized_mae_percent']:.2f}%")
    return metrics


def predict_seven_day_baseline(test: DataFrame) -> np.ndarray:
    return np.maximum(0, test["SalesTrailing7Sum"].to_numpy())


def build_model_pipeline() -> Pipeline:
    preprocessor = ColumnTransformer(
        transformers=[
            (
                "categories",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                CATEGORICAL_FEATURES,
            ),
            ("numbers", "passthrough", NUMERIC_FEATURES),
        ]
    )

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            (
                "model",
                HistGradientBoostingRegressor(
                    learning_rate=0.06,
                    max_iter=300,
                    max_leaf_nodes=31,
                    min_samples_leaf=30,
                    l2_regularization=1.0,
                    random_state=42,
                ),
            ),
        ]
    )


def analyze_prediction_errors(
    test: DataFrame,
    baseline_predictions: np.ndarray,
    model_predictions: np.ndarray,
) -> DataFrame:
    errors = test[
        [
            "Date",
            "Store ID",
            "Product ID",
            "Product Name",
            "Category",
            "Store Type",
            TARGET,
        ]
    ].copy()
    errors["SevenDayBaselinePrediction"] = baseline_predictions
    errors["ModelPrediction"] = model_predictions
    errors["ModelError"] = errors["ModelPrediction"] - errors[TARGET]
    errors["ModelAbsoluteError"] = errors["ModelError"].abs()

    print("\nCommon prediction errors")
    print("Largest under-predictions:")
    under = errors.sort_values(by="ModelError").head(5)
    print(
        under[
            [
                "Date",
                "Store ID",
                "Product ID",
                "Category",
                TARGET,
                "ModelPrediction",
                "ModelError",
            ]
        ].to_string(index=False)
    )

    print("\nLargest over-predictions:")
    over = errors.sort_values(by="ModelError", ascending=False).head(5)
    print(
        over[
            [
                "Date",
                "Store ID",
                "Product ID",
                "Category",
                TARGET,
                "ModelPrediction",
                "ModelError",
            ]
        ].to_string(index=False)
    )

    print("\nMean absolute error by category:")
    print(
        errors.groupby("Category")["ModelAbsoluteError"]
        .mean()
        .sort_values(ascending=False)
        .head(10)
        .round(2)
        .to_string()
    )

    print("\nMean absolute error by store type:")
    print(
        errors.groupby("Store Type")["ModelAbsoluteError"]
        .mean()
        .sort_values(ascending=False)
        .round(2)
        .to_string()
    )

    return DataFrame(errors)


def save_outputs(
    pipeline: Pipeline,
    errors: DataFrame,
    model_metrics: dict[str, float],
    baseline_metrics: dict[str, float],
    test_start: pd.Timestamp,
    train_cutoff: pd.Timestamp,
    training_seconds: float,
) -> None:
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(
        {
            "pipeline": pipeline,
            "features": FEATURES,
            "categorical_features": CATEGORICAL_FEATURES,
            "numeric_features": NUMERIC_FEATURES,
            "target": TARGET,
            "horizon_days": HORIZON_DAYS,
            "test_days": TEST_DAYS,
            "limited_storage_filter": {
                "has_warehouse": 0,
                "preferred_horizon_days_max": HORIZON_DAYS,
            },
            "test_start": str(test_start.date()),
            "train_cutoff": str(train_cutoff.date()),
            "training_seconds": training_seconds,
            "model_metrics": model_metrics,
            "baseline_metrics": baseline_metrics,
            "data_label": "synthetic",
        },
        MODEL_PATH,
    )

    metrics_rows = [
        {
            "model": "seven_day_trailing_sales_baseline",
            "training_seconds": 0.0,
            **baseline_metrics,
        },
        {
            "model": "hist_gradient_boosting",
            "training_seconds": training_seconds,
            **model_metrics,
        },
    ]
    pd.DataFrame(metrics_rows).to_csv(METRICS_PATH, index=False)
    errors.to_csv(ERRORS_PATH, index=False)

    print("\nSaved outputs")
    print(f"Model saved to: {MODEL_PATH}")
    print(f"Metrics saved to: {METRICS_PATH}")
    print(f"Prediction errors saved to: {ERRORS_PATH}")


def main() -> None:
    df = load_target_data()
    eligible = filter_limited_storage_stores(df)
    featured = add_historical_features(eligible)
    validate_feature_columns(featured)
    train, test, test_start, train_cutoff = split_future_dates(featured)

    print("7-day limited-storage demand model")
    print(f"Dataset path: {DATA_PATH}")
    print(f"Rows with {TARGET}: {len(df):,}")
    print(f"Date range: {df['Date'].min().date()} to {df['Date'].max().date()}")
    print(f"Target: {TARGET}")
    print(f"Horizon days: {HORIZON_DAYS}")
    print("\nEligible limited-storage stores")
    print(f"Rows: {len(eligible):,}")
    print(f"Stores: {eligible['Store ID'].nunique():,}")
    print(f"Products: {eligible['Product ID'].nunique():,}")
    print(f"Store types: {', '.join(sorted(eligible['Store Type'].unique()))}")
    print(
        "Date range: "
        f"{eligible['Date'].min().date()} to {eligible['Date'].max().date()}"
    )
    print(f"Rows after historical features: {len(featured):,}")
    print(f"Training rows: {len(train):,}")
    print(f"Testing rows: {len(test):,}")
    print(f"Training ends before: {train_cutoff.date()}")
    print(f"Test period: {test_start.date()} to {test['Date'].max().date()}")
    print("Excluded features:", ", ".join(EXCLUDED_FEATURES))

    baseline_predictions = predict_seven_day_baseline(test)
    baseline_metrics = evaluate(
        "Trailing 7-day sales baseline",
        test[TARGET],
        baseline_predictions,
    )

    pipeline = build_model_pipeline()
    print("\nTraining 7-day limited-storage demand model...")
    started = perf_counter()
    pipeline.fit(train[FEATURES], train[TARGET])
    training_seconds = perf_counter() - started

    model_predictions = np.maximum(0, pipeline.predict(test[FEATURES]))
    model_metrics = evaluate(
        "Hist Gradient Boosting 7-day model",
        test[TARGET],
        model_predictions,
    )
    improvement = (
        (baseline_metrics["mae"] - model_metrics["mae"])
        / baseline_metrics["mae"]
        * 100
    )

    print(f"MAE improvement over baseline: {improvement:.1f}%")
    print(f"Training time: {training_seconds:.1f} seconds")

    errors = analyze_prediction_errors(test, baseline_predictions, model_predictions)
    save_outputs(
        pipeline,
        errors,
        model_metrics,
        baseline_metrics,
        test_start,
        train_cutoff,
        training_seconds,
    )


if __name__ == "__main__":
    main()

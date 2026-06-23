"""Train a leakage-safe 14-day demand model for large stores with stockrooms."""

from pathlib import Path

import numpy as np
import pandas as pd


DATA_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "processed"
    / "depotiq_multiday_targets.csv"
)
TARGET = "DemandNext14Days"
HORIZON_DAYS = 14
ELIGIBLE_STORE_TYPES = ["Large", "Warehouse Store"]
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


def load_target_data() -> pd.DataFrame:
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
    return complete


def filter_large_stockroom_stores(df: pd.DataFrame) -> pd.DataFrame:
    required_columns = ["Store Type", "Has Warehouse", "Preferred Horizon Days"]
    missing = [column for column in required_columns if column not in df.columns]
    if missing:
        raise ValueError(f"Missing required store columns: {', '.join(missing)}")

    eligible = df[
        (df["Store Type"].isin(ELIGIBLE_STORE_TYPES))
        & (df["Has Warehouse"] == 1)
        & (df["Preferred Horizon Days"] >= HORIZON_DAYS)
    ].copy()

    if eligible.empty:
        raise ValueError("No eligible large stores with stockrooms were found.")

    return eligible.reset_index(drop=True)


def add_historical_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.sort_values([*GROUP_COLUMNS, "Date"]).copy()
    sales = df.groupby(GROUP_COLUMNS, sort=False)["Units Sold"]

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

    return df.dropna(subset=FEATURES + [TARGET]).reset_index(drop=True)


def validate_feature_columns(df: pd.DataFrame) -> None:
    missing = [column for column in FEATURES if column not in df.columns]
    if missing:
        raise ValueError(f"Missing feature columns: {', '.join(missing)}")

    leaked = [column for column in EXCLUDED_FEATURES if column in FEATURES]
    if leaked:
        raise ValueError(f"Leakage columns included as features: {', '.join(leaked)}")


def main() -> None:
    df = load_target_data()
    eligible = filter_large_stockroom_stores(df)
    featured = add_historical_features(eligible)
    validate_feature_columns(featured)

    print("14-day large-store demand model")
    print(f"Dataset path: {DATA_PATH}")
    print(f"Rows with {TARGET}: {len(df):,}")
    print(f"Date range: {df['Date'].min().date()} to {df['Date'].max().date()}")
    print(f"Target: {TARGET}")
    print(f"Horizon days: {HORIZON_DAYS}")
    print("\nEligible large stores with stockrooms")
    print(f"Rows: {len(eligible):,}")
    print(f"Stores: {eligible['Store ID'].nunique():,}")
    print(f"Products: {eligible['Product ID'].nunique():,}")
    print(f"Store types: {', '.join(sorted(eligible['Store Type'].unique()))}")
    print(
        "Date range: "
        f"{eligible['Date'].min().date()} to {eligible['Date'].max().date()}"
    )
    print(f"Rows after historical features: {len(featured):,}")
    print("Excluded features:", ", ".join(EXCLUDED_FEATURES))


if __name__ == "__main__":
    main()

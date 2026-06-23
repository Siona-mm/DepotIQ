"""Train a leakage-safe 14-day demand model for large stores with stockrooms."""

from pathlib import Path

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


def main() -> None:
    df = load_target_data()
    eligible = filter_large_stockroom_stores(df)

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


if __name__ == "__main__":
    main()

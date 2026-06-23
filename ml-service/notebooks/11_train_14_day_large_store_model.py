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


def main() -> None:
    df = load_target_data()

    print("14-day large-store demand model")
    print(f"Dataset path: {DATA_PATH}")
    print(f"Rows with {TARGET}: {len(df):,}")
    print(f"Date range: {df['Date'].min().date()} to {df['Date'].max().date()}")
    print(f"Target: {TARGET}")
    print(f"Horizon days: {HORIZON_DAYS}")


if __name__ == "__main__":
    main()

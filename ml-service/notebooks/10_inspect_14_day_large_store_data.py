"""Inspect data readiness for the 14-day large-store demand model."""

from pathlib import Path

import pandas as pd


DATA_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "processed"
    / "depotiq_multiday_targets.csv"
)
TARGET = "DemandNext14Days"
REQUIRED_COLUMNS = [
    "Date",
    "Store ID",
    "Product ID",
    "Store Type",
    "Has Warehouse",
    "Preferred Horizon Days",
    "Units Sold",
    TARGET,
]


def main() -> None:
    if not DATA_PATH.exists():
        raise FileNotFoundError(
            f"Missing processed dataset at {DATA_PATH}. "
            "Run 08_create_multiday_targets.py before this inspection."
        )

    df = pd.read_csv(DATA_PATH, parse_dates=["Date"])
    missing = [column for column in REQUIRED_COLUMNS if column not in df.columns]
    if missing:
        raise ValueError(f"Missing required columns: {', '.join(missing)}")

    eligible = df[
        (df["Store Type"].isin(["Large", "Warehouse Store"]))
        & (df["Has Warehouse"] == 1)
        & (df["Preferred Horizon Days"] >= 14)
    ].copy()

    print("14-day large-store data inspection")
    print(f"Dataset path: {DATA_PATH}")
    print(f"Rows: {len(df):,}")
    print(f"Columns: {len(df.columns):,}")
    print(f"Date range: {df['Date'].min().date()} to {df['Date'].max().date()}")
    print(f"Target: {TARGET}")
    print(f"Missing target values: {int(df[TARGET].isna().sum()):,}")

    print("\nStore type counts:")
    print(df["Store Type"].value_counts().to_string())

    print("\nWarehouse flag counts:")
    print(df["Has Warehouse"].value_counts().sort_index().to_string())

    print("\nEligible large-store rows:")
    print(f"Rows: {len(eligible):,}")
    print(f"Stores: {eligible['Store ID'].nunique():,}")
    print(f"Products: {eligible['Product ID'].nunique():,}")
    print(
        "Date range: "
        f"{eligible['Date'].min().date()} to {eligible['Date'].max().date()}"
    )
    print(f"Average {TARGET}: {eligible[TARGET].mean():.2f}")
    print(f"Median {TARGET}: {eligible[TARGET].median():.2f}")


if __name__ == "__main__":
    main()

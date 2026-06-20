"""Create and validate leakage-safe multi-day demand targets."""

from pathlib import Path

import numpy as np
import pandas as pd


DATA_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "synthetic"
    / "depotiq_synthetic_retail.csv"
)
OUTPUT_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "processed"
    / "depotiq_multiday_targets.csv"
)
HORIZONS = (3, 7, 14, 30)
RANDOM_SEED = 42


def future_sales_sum(series: pd.Series, horizon: int) -> pd.Series:
    """Sum the next `horizon` values, excluding the current row."""
    return (
        series.shift(-1)
        .rolling(window=horizon, min_periods=horizon)
        .sum()
        .shift(-(horizon - 1))
    )


def add_targets(df: pd.DataFrame) -> pd.DataFrame:
    df = df.sort_values(["Store ID", "Product ID", "Date"]).reset_index(drop=True)
    grouped_sales = df.groupby(["Store ID", "Product ID"], sort=False)["Units Sold"]

    for horizon in HORIZONS:
        column = f"DemandNext{horizon}Days"
        df[column] = grouped_sales.transform(
            lambda series, days=horizon: future_sales_sum(series, days)
        )

    return df


def validate_random_rows(source: pd.DataFrame, targets: pd.DataFrame, sample_size: int = 5) -> None:
    rng = np.random.default_rng(RANDOM_SEED)
    sample_positions = rng.choice(len(targets), size=min(sample_size, len(targets)), replace=False)

    source_lookup = {
        key: group.sort_values("Date").reset_index(drop=True)
        for key, group in source.groupby(["Store ID", "Product ID"])
    }

    print("\nManual target checks:")
    for position in sample_positions:
        row = targets.iloc[position]
        key = (row["Store ID"], row["Product ID"])
        group = source_lookup[key]
        current_positions = group.index[group["Date"] == row["Date"]].tolist()

        if len(current_positions) != 1:
            raise ValueError(f"Expected one source row for {key} on {row['Date']}")

        current_position = current_positions[0]
        print(f"\n{key[0]} / {key[1]} / {row['Date'].date()}")
        print(f"Today's sales (excluded): {int(row['Units Sold'])}")

        for horizon in HORIZONS:
            column = f"DemandNext{horizon}Days"
            expected = group.iloc[
                current_position + 1 : current_position + horizon + 1
            ]["Units Sold"].sum()
            actual = row[column]

            if actual != expected:
                raise AssertionError(
                    f"{column} failed for {key} on {row['Date']}: "
                    f"expected {expected}, received {actual}"
                )

            print(f"{column}: {int(actual)} (verified)")


def main() -> None:
    print("Loading synthetic daily data...")
    source = pd.read_csv(DATA_PATH, parse_dates=["Date"])
    rows_before = len(source)

    with_targets = add_targets(source.copy())
    target_columns = [f"DemandNext{horizon}Days" for horizon in HORIZONS]

    # Keeping only complete rows means every remaining row has all four targets.
    complete = with_targets.dropna(subset=target_columns).copy()
    complete[target_columns] = complete[target_columns].astype(int)
    complete = complete.reset_index(drop=True)

    validate_random_rows(source, complete)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    complete.to_csv(OUTPUT_PATH, index=False)

    expected_removed = source[["Store ID", "Product ID"]].drop_duplicates().shape[0] * max(HORIZONS)

    print("\nMulti-day targets created successfully")
    print(f"Input rows: {rows_before:,}")
    print(f"Output rows: {len(complete):,}")
    print(f"Rows removed: {rows_before - len(complete):,}")
    print(f"Expected rows removed: {expected_removed:,}")
    print("Target columns:", ", ".join(target_columns))
    print(f"Missing target values: {int(complete[target_columns].isna().sum().sum())}")
    print(f"Saved to: {OUTPUT_PATH}")
    print("\nImportant: target columns are labels and must never be model features.")


if __name__ == "__main__":
    main()

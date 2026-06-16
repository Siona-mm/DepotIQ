from pathlib import Path

import joblib
import pandas as pd

DATA_PATH = Path("../../data/raw/retail_store_inventory.csv")
MODEL_PATH = Path("../models/demand_model.joblib")
PLANNING_DAYS = 7
SAFETY_STOCK_RATIO = 0.15

FEATURES = [
    "Store ID",
    "Product ID",
    "Category",
    "Region",
    "Inventory Level",
    "Units Ordered",
    "Price",
    "Discount",
    "Weather Condition",
    "Holiday/Promotion",
    "Competitor Pricing",
    "Seasonality",
    "DayOfWeek",
    "Month",
    "Year",
]


def add_date_features(df):
    df = df.copy()
    df["Date"] = pd.to_datetime(df["Date"])
    df["DayOfWeek"] = df["Date"].dt.dayofweek
    df["Month"] = df["Date"].dt.month
    df["Year"] = df["Date"].dt.year
    return df


def get_priority(row):
    shipment = row["Recommended Shipment"]
    inventory = row["Inventory Level"]
    target = row["Target Stock"]

    if shipment <= 0:
        return "Low"
    if inventory <= target * 0.25:
        return "Urgent"
    if inventory <= target * 0.60:
        return "High"
    return "Normal"


def main():
    model = joblib.load(MODEL_PATH)
    df = add_date_features(pd.read_csv(DATA_PATH))

    df["Predicted Demand"] = model.predict(df[FEATURES]).round().astype(int)
    df["Forecast Horizon Demand"] = df["Predicted Demand"] * PLANNING_DAYS
    df["Safety Stock"] = (df["Forecast Horizon Demand"] * SAFETY_STOCK_RATIO).round().astype(int)
    df["Target Stock"] = df["Forecast Horizon Demand"] + df["Safety Stock"]
    df["Recommended Shipment"] = (df["Target Stock"] - df["Inventory Level"]).clip(lower=0)
    df["Priority"] = df.apply(get_priority, axis=1)

    output_columns = [
        "Store ID",
        "Product ID",
        "Category",
        "Inventory Level",
        "Units Sold",
        "Predicted Demand",
        "Forecast Horizon Demand",
        "Safety Stock",
        "Recommended Shipment",
        "Priority",
    ]

    recommendations = df[df["Recommended Shipment"] > 0].sort_values(
        by=["Recommended Shipment", "Predicted Demand"],
        ascending=False,
    )

    print(f"Top shipment recommendations for the next {PLANNING_DAYS} days:")
    print(recommendations[output_columns].head(20).to_string(index=False))

    print("\nPriority counts:")
    print(df["Priority"].value_counts().to_string())


if __name__ == "__main__":
    main()

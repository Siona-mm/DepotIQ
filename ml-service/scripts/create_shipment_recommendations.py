from pathlib import Path

import joblib
import numpy as np
import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parents[2]

DATA_PATH = PROJECT_ROOT / "data" / "processed" / "depotiq_multiday_targets.csv"
OUTPUT_PATH = PROJECT_ROOT / "reports" / "shipment_recommendations.csv"
MODELS_DIR = PROJECT_ROOT / "ml-service" / "models"

SAFETY_STOCK_RATIO = 0.15

MODEL_FILES = {
    3: MODELS_DIR / "synthetic_honest_3_day_model.joblib",
    7: MODELS_DIR / "demand_model_7_day.joblib",
    14: MODELS_DIR / "large_store_14_day_demand_model.joblib",
    30: MODELS_DIR / "demand_model_30_day.joblib",
}


def load_data():
    if not DATA_PATH.exists():
        raise FileNotFoundError(
            f"Missing dataset: {DATA_PATH}\n"
            "Run notebooks/08_create_multiday_targets.py first."
        )

    return pd.read_csv(DATA_PATH, parse_dates=["Date"])


def add_historical_features(df):
    df = df.sort_values(["Store ID", "Product ID", "Date"]).copy()
    sales = df.groupby(["Store ID", "Product ID"])["Units Sold"]

    for lag in [1, 7, 14, 30]:
        df[f"SalesLag{lag}"] = sales.shift(lag)

    for window in [7, 14, 30]:
        df[f"SalesRolling{window}"] = sales.transform(
            lambda x: x.shift(1).rolling(window).mean()
        )

    df["SalesTrailing7Sum"] = sales.transform(
        lambda x: x.shift(1).rolling(7).sum()
    )

    df["DayOfWeek"] = df["Date"].dt.dayofweek
    df["Month"] = df["Date"].dt.month
    df["DayOfYearSin"] = np.sin(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["DayOfYearCos"] = np.cos(2 * np.pi * df["Date"].dt.dayofyear / 365.25)
    df["IsWeekend"] = (df["DayOfWeek"] >= 5).astype(int)

    return df


def choose_horizon(row):
    store_type = row["Store Type"]
    has_warehouse = int(row["Has Warehouse"])
    preferred_horizon = int(row["Preferred Horizon Days"])

    if store_type == "Warehouse Store" and has_warehouse == 1 and preferred_horizon >= 30:
        return 30

    if store_type == "Large" and has_warehouse == 1 and preferred_horizon >= 14:
        return 14

    if store_type == "Medium":
        return 7

    return 3


def load_model(horizon):
    model_path = MODEL_FILES[horizon]

    if not model_path.exists():
        raise FileNotFoundError(
            f"Missing model for {horizon}-day horizon: {model_path}"
        )

    model_artifact = joblib.load(model_path)

    if isinstance(model_artifact, dict):
        return model_artifact["pipeline"], model_artifact["features"]

    raise ValueError(
        f"Unexpected model format for {horizon}-day model. "
        "Expected a dictionary with pipeline and features."
    )


def predict_for_horizon(snapshot, horizon):
    rows = snapshot[snapshot["Horizon Days"] == horizon].copy()

    if rows.empty:
        return rows

    pipeline, features = load_model(horizon)

    missing_features = [feature for feature in features if feature not in rows.columns]
    if missing_features:
        raise ValueError(
            f"Missing features for {horizon}-day model: {missing_features}"
        )

    rows["Predicted Demand"] = pipeline.predict(rows[features])
    rows["Predicted Demand"] = rows["Predicted Demand"].clip(lower=0).round().astype(int)

    return rows


def assign_priority(row):
    predicted_demand = row["Predicted Demand"]
    horizon_days = row["Horizon Days"]
    current_inventory = row["Inventory Level"]
    delivery_lead_time = row["Delivery Lead Time"]
    recommended_shipment = row["Recommended Shipment"]

    if recommended_shipment <= 0:
        return "Low"

    if predicted_demand <= 0:
        return "Low"

    daily_demand = predicted_demand / horizon_days
    days_of_stock_left = current_inventory / daily_demand

    if days_of_stock_left <= delivery_lead_time:
        return "Urgent"

    if days_of_stock_left <= horizon_days * 0.25:
        return "High"

    return "Normal"


def create_recommendations(df):
    latest_date = df["Date"].max()
    snapshot = df[df["Date"] == latest_date].copy()

    snapshot["Horizon Days"] = snapshot.apply(choose_horizon, axis=1)

    prediction_frames = []

    for horizon in [3, 7, 14, 30]:
        prediction_frames.append(predict_for_horizon(snapshot, horizon))

    predictions = pd.concat(prediction_frames, ignore_index=True)

    predictions["Safety Stock"] = (
        predictions["Predicted Demand"] * SAFETY_STOCK_RATIO
    ).round().astype(int)

    predictions["Required Stock"] = (
        predictions["Predicted Demand"] + predictions["Safety Stock"]
    )

    predictions["Available Stock"] = (
        predictions["Inventory Level"] + predictions["Incoming Units"]
    )

    predictions["Raw Recommended Shipment"] = (
        predictions["Required Stock"] - predictions["Available Stock"]
    ).clip(lower=0)

    predictions["Remaining Store Capacity"] = (
        predictions["Storage Capacity"] - predictions["Available Stock"]
    ).clip(lower=0)

    predictions["Recommended Shipment"] = np.minimum(
        predictions["Raw Recommended Shipment"],
        predictions["Remaining Store Capacity"]
    ).round().astype(int)

    predictions["Priority"] = predictions.apply(assign_priority, axis=1)

    output_columns = [
        "Date",
        "Store ID",
        "Store Type",
        "Product ID",
        "Product Name",
        "Category",
        "Horizon Days",
        "Inventory Level",
        "Incoming Units",
        "Storage Capacity",
        "Delivery Lead Time",
        "Predicted Demand",
        "Safety Stock",
        "Required Stock",
        "Available Stock",
        "Remaining Store Capacity",
        "Recommended Shipment",
        "Priority",
    ]

    recommendations = predictions[output_columns].copy()

    priority_order = {
        "Urgent": 0,
        "High": 1,
        "Normal": 2,
        "Low": 3,
    }

    recommendations["Priority Order"] = recommendations["Priority"].map(priority_order)

    recommendations = recommendations.sort_values(
        by=["Priority Order", "Recommended Shipment"],
        ascending=[True, False]
    ).drop(columns=["Priority Order"])

    return recommendations


def main():
    df = load_data()
    df = add_historical_features(df)

    recommendations = create_recommendations(df)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    recommendations.to_csv(OUTPUT_PATH, index=False)

    print("Shipment recommendations created.")
    print(f"Saved to: {OUTPUT_PATH}")
    print()
    print("Priority counts:")
    print(recommendations["Priority"].value_counts())
    print()
    print("Top recommendations:")
    print(
        recommendations[
            [
                "Store ID",
                "Store Type",
                "Product ID",
                "Category",
                "Horizon Days",
                "Predicted Demand",
                "Recommended Shipment",
                "Priority",
            ]
        ].head(20).to_string(index=False)
    )


if __name__ == "__main__":
    main()
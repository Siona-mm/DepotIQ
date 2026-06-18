from pathlib import Path

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import HistGradientBoostingRegressor, RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder


DATA_PATH = Path("../../data/raw/retail_store_inventory.csv")
MODEL_PATH = Path("../models/honest_demand_model.joblib")


TARGET = "Units Sold"
GROUP_COLUMNS = ["Store ID", "Product ID"]

CATEGORICAL_FEATURES = [
    "Store ID",
    "Product ID",
    "Category",
    "Region",
    "Weather Condition",
    "Seasonality",
]

NUMERIC_FEATURES = [
    "Price",
    "Discount",
    "Holiday/Promotion",
    "Competitor Pricing",
    "DayOfWeek",
    "Month",
    "Year",
    "IsWeekend",
    "SalesLag1",
    "SalesLag7",
    "SalesRolling7",
    "SalesRolling14",
    "SalesRolling30",
]

FEATURES = CATEGORICAL_FEATURES + NUMERIC_FEATURES

def add_features(df):
    df = df.copy()

    df["Date"] = pd.to_datetime(df["Date"])
    df = df.sort_values(["Store ID", "Product ID", "Date"])

    df["DayOfWeek"] = df["Date"].dt.dayofweek
    df["Month"] = df["Date"].dt.month
    df["Year"] = df["Date"].dt.year
    df["IsWeekend"] = df["DayOfWeek"].isin([5, 6]).astype(int)

    grouped_sales = df.groupby(GROUP_COLUMNS)[TARGET]

    df["SalesLag1"] = grouped_sales.shift(1)
    df["SalesLag7"] = grouped_sales.shift(7)
    df["SalesRolling7"] = grouped_sales.transform(
        lambda sales: sales.shift(1).rolling(7).mean()
    )
    df["SalesRolling14"] = grouped_sales.transform(
        lambda sales: sales.shift(1).rolling(14).mean()
    )
    df["SalesRolling30"] = grouped_sales.transform(
        lambda sales: sales.shift(1).rolling(30).mean()
    )

    return df.dropna(
        subset=[
            "SalesLag1",
            "SalesLag7",
            "SalesRolling7",
            "SalesRolling14",
            "SalesRolling30",
        ]
    )

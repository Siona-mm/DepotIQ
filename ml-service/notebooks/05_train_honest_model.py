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
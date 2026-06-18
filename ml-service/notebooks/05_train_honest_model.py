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

def evaluate_model(name, model, X_train, X_test, y_train, y_test):
    preprocessor = ColumnTransformer(
        transformers=[
            ("categorical", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL_FEATURES),
            ("numeric", "passthrough", NUMERIC_FEATURES),
        ]
    )

    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("model", model),
        ]
    )

    print(f"\nTraining {name}...")
    pipeline.fit(X_train, y_train)

    predictions = pipeline.predict(X_test)

    mae = mean_absolute_error(y_test, predictions)
    rmse = mean_squared_error(y_test, predictions) ** 0.5
    r2 = r2_score(y_test, predictions)

    print(f"{name} results:")
    print(f"MAE: {mae:.2f}")
    print(f"RMSE: {rmse:.2f}")
    print(f"R2 Score: {r2:.3f}")

    return {
        "name": name,
        "pipeline": pipeline,
        "mae": mae,
        "rmse": rmse,
        "r2": r2,
    }



def main():
    if not DATA_PATH.exists():
        raise FileNotFoundError(
            f"Dataset not found at {DATA_PATH}. "
            "Download it from Kaggle and place it in data/raw/retail_store_inventory.csv"
        )

    df = pd.read_csv(DATA_PATH)

    forbidden_features = {"Units Ordered", "Demand Forecast"}
    used_forbidden_features = forbidden_features.intersection(FEATURES)

    if used_forbidden_features:
        raise ValueError(f"Honest model is using forbidden features: {used_forbidden_features}")

    df = add_features(df)

    train_cutoff = pd.Timestamp("2023-10-01")
    train_df = df[df["Date"] < train_cutoff]
    test_df = df[df["Date"] >= train_cutoff]

    X_train = train_df[FEATURES]
    y_train = train_df[TARGET]
    X_test = test_df[FEATURES]
    y_test = test_df[TARGET]

    print("Honest demand model")
    print("Excluded features: Units Ordered, Demand Forecast")
    print(f"Training rows: {len(train_df)}")
    print(f"Testing rows: {len(test_df)}")
    print(f"Test period: {test_df['Date'].min().date()} to {test_df['Date'].max().date()}")

    candidates = [
        (
            "Random Forest",
            RandomForestRegressor(
                n_estimators=200,
                min_samples_leaf=2,
                random_state=42,
                n_jobs=-1,
            ),
        ),
        (
            "Hist Gradient Boosting",
            HistGradientBoostingRegressor(
                max_iter=300,
                learning_rate=0.05,
                max_leaf_nodes=31,
                random_state=42,
            ),
        ),
    ]

    results = [
        evaluate_model(name, model, X_train, X_test, y_train, y_test)
        for name, model in candidates
    ]

    best = min(results, key=lambda result: result["mae"])

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(best["pipeline"], MODEL_PATH)

    print("\nBest honest model:")
    print(best["name"])
    print(f"MAE: {best['mae']:.2f}")
    print(f"RMSE: {best['rmse']:.2f}")
    print(f"R2 Score: {best['r2']:.3f}")
    print(f"Model saved to: {MODEL_PATH}")


if __name__ == "__main__":
    main()
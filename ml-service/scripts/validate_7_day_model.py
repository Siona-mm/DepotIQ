"""Validate the saved 7-day demand model artifact."""

from pathlib import Path
import sys
import warnings

import joblib
from sklearn.exceptions import InconsistentVersionWarning


MODEL_PATH = (
    Path(__file__).resolve().parents[1]
    / "models"
    / "demand_model_7_day.joblib"
)
LEAKAGE_FEATURES = {
    "Demand Forecast",
    "Units Ordered",
    "Incoming Units",
    "True Demand",
    "Stockout Units",
    "DemandNext3Days",
    "DemandNext7Days",
    "DemandNext14Days",
    "DemandNext30Days",
}


def load_model_artifact() -> dict:
    if not MODEL_PATH.exists():
        raise FileNotFoundError(
            f"Missing model artifact at {MODEL_PATH}. "
            "Run ml-service/notebooks/12_train_7_day_demand_model.py first."
        )

    try:
        with warnings.catch_warnings():
            warnings.filterwarnings("error", category=InconsistentVersionWarning)
            return joblib.load(MODEL_PATH)
    except InconsistentVersionWarning as exc:
        raise RuntimeError(
            "The saved model was created with a different scikit-learn version. "
            "Retrain it with ml-service/notebooks/12_train_7_day_demand_model.py "
            "using this virtualenv, then run this validator again."
        ) from exc
    except ModuleNotFoundError as exc:
        raise RuntimeError(
            "The saved model could not be loaded by this Python environment. "
            "Retrain it with ml-service/notebooks/12_train_7_day_demand_model.py "
            "using this virtualenv, then run this validator again."
        ) from exc


def main() -> int:
    try:
        artifact = load_model_artifact()
    except (FileNotFoundError, RuntimeError) as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1

    leakage_features = sorted(set(artifact["features"]) & LEAKAGE_FEATURES)

    print("model path:", MODEL_PATH)
    print("target:", artifact["target"])
    print("horizon:", artifact["horizon_days"])
    print("model MAE:", round(artifact["model_metrics"]["mae"], 2))
    print("baseline MAE:", round(artifact["baseline_metrics"]["mae"], 2))
    print("leakage features:", leakage_features)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

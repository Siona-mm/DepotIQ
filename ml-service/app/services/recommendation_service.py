from datetime import datetime, timezone
from functools import lru_cache

from app.schemas.recommendation import (
    ModelInfoResponse,
    RecommendationBatchResponse,
    RecommendationResponse,
)
from scripts.create_shipment_recommendations import (
    MODEL_FILES,
    add_historical_features,
    create_recommendations,
    load_data,
)


MODEL_METADATA = {
    3: {"mae": 11.85, "rmse": 17.91},
    7: {"mae": 17.14, "rmse": 25.52},
    14: {"mae": 41.58, "rmse": 61.04},
    30: {"mae": 105.73, "rmse": 151.34},
}
MODEL_NAME = "hist_gradient_boosting"
MODEL_VERSION = "1.0"


def _to_response(row) -> RecommendationResponse:
    horizon = int(row["Horizon Days"])
    predicted_demand = int(row["Predicted Demand"])
    model_mae = MODEL_METADATA[horizon]["mae"]
    confidence_lower = max(0, round(predicted_demand - model_mae))
    confidence_upper = round(predicted_demand + model_mae)

    return RecommendationResponse(
        storeCode=str(row["Store ID"]),
        productCode=str(row["Product ID"]),
        storeType=str(row["Store Type"]),
        category=str(row["Category"]),
        forecastDate=row["Date"].date(),
        horizonDays=horizon,
        currentInventory=int(row["Inventory Level"]),
        incomingUnits=int(row["Incoming Units"]),
        predictedDemand=predicted_demand,
        confidenceLower=confidence_lower,
        confidenceUpper=confidence_upper,
        modelName=MODEL_NAME,
        modelVersion=MODEL_VERSION,
        modelMae=model_mae,
        safetyStock=int(row["Safety Stock"]),
        requiredStock=int(row["Required Stock"]),
        recommendedShipment=int(row["Recommended Shipment"]),
        priority=str(row["Priority"]).upper(),
        explanation=(
            f"{horizon}-day demand forecast with 15% safety stock. "
            "The displayed range is based on validation MAE and is not a "
            "formal statistical confidence interval."
        ),
    )


@lru_cache(maxsize=1)
def build_recommendations() -> tuple[RecommendationResponse, ...]:
    data = add_historical_features(load_data())
    recommendations = create_recommendations(data)
    return tuple(_to_response(row) for _, row in recommendations.iterrows())


def get_recommendations(
    store_code: str | None = None,
    product_code: str | None = None,
    limit: int = 250,
) -> RecommendationBatchResponse:
    recommendations = list(build_recommendations())

    if store_code:
        recommendations = [
            item for item in recommendations if item.storeCode == store_code
        ]

    if product_code:
        recommendations = [
            item for item in recommendations if item.productCode == product_code
        ]

    total_available = len(recommendations)
    selected = recommendations[:limit]
    source_date = selected[0].forecastDate if selected else load_data()["Date"].max().date()

    return RecommendationBatchResponse(
        generatedAt=datetime.now(timezone.utc),
        sourceDate=source_date,
        totalAvailable=total_available,
        returned=len(selected),
        recommendations=selected,
    )


def refresh_recommendations() -> int:
    build_recommendations.cache_clear()
    return len(build_recommendations())


def get_model_info() -> list[ModelInfoResponse]:
    return [
        ModelInfoResponse(
            horizonDays=horizon,
            modelName=MODEL_NAME,
            modelVersion=MODEL_VERSION,
            mae=metrics["mae"],
            rmse=metrics["rmse"],
            artifactAvailable=MODEL_FILES[horizon].exists(),
        )
        for horizon, metrics in MODEL_METADATA.items()
    ]

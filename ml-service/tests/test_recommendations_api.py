from datetime import date, datetime, timezone

from fastapi.testclient import TestClient

from app.routes import recommendations as recommendation_routes
from app.schemas.recommendation import (
    ModelInfoResponse,
    RecommendationBatchResponse,
    RecommendationResponse,
)
from main import app


client = TestClient(app)


def sample_recommendation():
    return RecommendationResponse(
        storeCode="S001",
        productCode="P0001",
        storeType="Small",
        category="Groceries",
        forecastDate=date(2024, 2, 4),
        horizonDays=3,
        currentInventory=20,
        incomingUnits=5,
        predictedDemand=60,
        confidenceLower=48,
        confidenceUpper=72,
        modelName="hist_gradient_boosting",
        modelVersion="1.0",
        modelMae=11.85,
        safetyStock=9,
        requiredStock=69,
        recommendedShipment=44,
        priority="URGENT",
        explanation="Validation-based range.",
    )


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_recommendations_endpoint(monkeypatch):
    batch = RecommendationBatchResponse(
        generatedAt=datetime.now(timezone.utc),
        sourceDate=date(2024, 2, 4),
        totalAvailable=1,
        returned=1,
        recommendations=[sample_recommendation()],
    )
    monkeypatch.setattr(
        recommendation_routes,
        "get_recommendations",
        lambda store_code, product_code, limit: batch,
    )

    response = client.get("/recommendations?store_code=S001&limit=10")

    assert response.status_code == 200
    body = response.json()
    assert body["returned"] == 1
    assert body["recommendations"][0]["recommendedShipment"] == 44


def test_models_endpoint(monkeypatch):
    monkeypatch.setattr(
        recommendation_routes,
        "get_model_info",
        lambda: [
            ModelInfoResponse(
                horizonDays=3,
                modelName="hist_gradient_boosting",
                modelVersion="1.0",
                mae=11.85,
                rmse=17.91,
                artifactAvailable=True,
            )
        ],
    )

    response = client.get("/recommendations/models")

    assert response.status_code == 200
    assert response.json()[0]["artifactAvailable"] is True

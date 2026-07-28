from fastapi import APIRouter, Query

from app.schemas.recommendation import (
    ModelInfoResponse,
    RecommendationBatchResponse,
    RefreshResponse,
)
from app.services.recommendation_service import (
    get_model_info,
    get_recommendations,
    refresh_recommendations,
)


router = APIRouter(prefix="/recommendations", tags=["recommendations"])


@router.get("", response_model=RecommendationBatchResponse)
def list_recommendations(
    store_code: str | None = Query(default=None),
    product_code: str | None = Query(default=None),
    limit: int = Query(default=250, ge=1, le=1000),
):
    return get_recommendations(store_code, product_code, limit)


@router.post("/refresh", response_model=RefreshResponse)
def refresh():
    generated = refresh_recommendations()
    return RefreshResponse(
        status="refreshed",
        recommendationsGenerated=generated,
    )


@router.get("/models", response_model=list[ModelInfoResponse])
def models():
    return get_model_info()

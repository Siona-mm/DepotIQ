from datetime import date, datetime

from pydantic import BaseModel, Field


class RecommendationResponse(BaseModel):
    storeCode: str
    productCode: str
    storeType: str
    category: str
    forecastDate: date
    horizonDays: int
    currentInventory: int
    incomingUnits: int
    predictedDemand: int
    confidenceLower: int
    confidenceUpper: int
    modelName: str
    modelVersion: str
    modelMae: float
    safetyStock: int
    requiredStock: int
    recommendedShipment: int
    priority: str
    explanation: str


class RecommendationBatchResponse(BaseModel):
    generatedAt: datetime
    sourceDate: date
    totalAvailable: int
    returned: int
    recommendations: list[RecommendationResponse]


class ModelInfoResponse(BaseModel):
    horizonDays: int
    modelName: str
    modelVersion: str
    mae: float
    rmse: float
    artifactAvailable: bool


class RefreshResponse(BaseModel):
    status: str
    recommendationsGenerated: int = Field(ge=0)

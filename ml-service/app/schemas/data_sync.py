from datetime import date, datetime

from pydantic import BaseModel, Field


class SalesRecordPayload(BaseModel):
    storeCode: str
    productCode: str
    saleDate: date
    unitsSold: int = Field(ge=0)
    price: float | None = Field(default=None, ge=0)
    discount: float | None = Field(default=None, ge=0)
    promotion: bool
    weatherCondition: str | None = None
    holidayPromotion: bool
    seasonality: str | None = None


class StoreInventoryPayload(BaseModel):
    storeCode: str
    productCode: str
    inventoryLevel: int = Field(ge=0)
    incomingUnits: int = Field(ge=0)


class DataSyncRequest(BaseModel):
    syncedAt: datetime
    salesRecords: list[SalesRecordPayload]
    storeInventory: list[StoreInventoryPayload]


class DataSyncResponse(BaseModel):
    status: str
    salesRecordsReceived: int
    storeInventoryReceived: int
    syncedAt: datetime

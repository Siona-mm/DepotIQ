from fastapi import APIRouter

from app.schemas.data_sync import DataSyncRequest, DataSyncResponse
from app.services.data_sync_service import store_sync


router = APIRouter(prefix="/data-sync", tags=["data-sync"])


@router.post("", response_model=DataSyncResponse)
def sync_data(payload: DataSyncRequest) -> DataSyncResponse:
    return store_sync(payload)

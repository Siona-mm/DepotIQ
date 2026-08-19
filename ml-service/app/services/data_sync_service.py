from app.schemas.data_sync import DataSyncRequest, DataSyncResponse


latest_sync: DataSyncRequest | None = None


def store_sync(payload: DataSyncRequest) -> DataSyncResponse:
    global latest_sync
    latest_sync = payload
    from app.services.recommendation_service import clear_recommendation_cache

    clear_recommendation_cache()
    return DataSyncResponse(
        status="accepted",
        salesRecordsReceived=len(payload.salesRecords),
        storeInventoryReceived=len(payload.storeInventory),
        syncedAt=payload.syncedAt,
    )


def get_latest_sync() -> DataSyncRequest | None:
    return latest_sync

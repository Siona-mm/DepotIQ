from app.schemas.data_sync import DataSyncRequest, DataSyncResponse


latest_sync: DataSyncRequest | None = None


def store_sync(payload: DataSyncRequest) -> DataSyncResponse:
    global latest_sync
    latest_sync = payload
    return DataSyncResponse(
        status="accepted",
        salesRecordsReceived=len(payload.salesRecords),
        storeInventoryReceived=len(payload.storeInventory),
        syncedAt=payload.syncedAt,
    )

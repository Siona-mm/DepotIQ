package com.depotiq.dtos.ml;

import java.time.LocalDateTime;
import java.util.List;

public record MlDataSyncRequest(
        LocalDateTime syncedAt,
        List<MlSalesRecordPayload> salesRecords,
        List<MlStoreInventoryPayload> storeInventory
) {
}

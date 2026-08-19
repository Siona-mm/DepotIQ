package com.depotiq.dtos.ml;

import java.time.LocalDateTime;

public record MlDataSyncResponse(
        String status,
        Integer salesRecordsReceived,
        Integer storeInventoryReceived,
        LocalDateTime syncedAt
) {
}

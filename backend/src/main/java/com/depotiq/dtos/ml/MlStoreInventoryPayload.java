package com.depotiq.dtos.ml;

public record MlStoreInventoryPayload(
        String storeCode,
        String productCode,
        Integer inventoryLevel,
        Integer incomingUnits
) {
}

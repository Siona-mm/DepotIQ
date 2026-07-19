package com.depotiq.dtos.inventory.store;

import java.time.LocalDateTime;

public record StoreInventoryResponse(
        Long id,
        String storeId,
        String productId,
        Integer inventoryLevel,
        Integer reorderPoint,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

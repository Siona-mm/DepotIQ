package com.depotiq.dtos.inventory.depot;

import java.time.LocalDateTime;

public record DepotInventoryResponse(
        Long id,
        String depotId,
        String productId,
        Integer inventoryLevel,
        Integer reorderPoint,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

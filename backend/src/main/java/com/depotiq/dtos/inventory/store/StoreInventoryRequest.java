package com.depotiq.dtos.inventory.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StoreInventoryRequest(
        @NotBlank String storeId,
        @NotBlank String productId,
        @NotNull @PositiveOrZero Integer inventoryLevel,
        @NotNull @PositiveOrZero Integer reorderPoint
) {
}

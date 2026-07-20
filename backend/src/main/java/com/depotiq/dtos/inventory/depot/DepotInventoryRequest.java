package com.depotiq.dtos.inventory.depot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DepotInventoryRequest(
        @NotBlank String depotId,
        @NotBlank String productId,
        @NotNull @PositiveOrZero Integer inventoryLevel,
        @NotNull @PositiveOrZero Integer reorderPoint
) {
}

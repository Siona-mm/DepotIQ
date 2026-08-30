package com.depotiq.dtos.inventory;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

public class UpsertDepotInventoryRequest {
    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Min(0)
    private Integer availableUnits;

    @NotNull
    @Min(0)
    private Integer reservedUnits;

    @AssertTrue(message = "Reserved units cannot exceed available units")
    public boolean isReservationValid() {
        return availableUnits == null || reservedUnits == null || reservedUnits <= availableUnits;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Integer availableUnits) {
        this.availableUnits = availableUnits;
    }

    public Integer getReservedUnits() {
        return reservedUnits;
    }

    public void setReservedUnits(Integer reservedUnits) {
        this.reservedUnits = reservedUnits;
    }
}

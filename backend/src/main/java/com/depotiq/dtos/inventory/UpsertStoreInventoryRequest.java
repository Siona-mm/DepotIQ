package com.depotiq.dtos.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpsertStoreInventoryRequest {
    @NotNull
    private Long storeId;

    @NotNull
    private Long productId;

    @NotNull
    @Min(0)
    private Integer inventoryLevel;

    @NotNull
    @Min(0)
    private Integer incomingUnits;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getInventoryLevel() {
        return inventoryLevel;
    }

    public void setInventoryLevel(Integer inventoryLevel) {
        this.inventoryLevel = inventoryLevel;
    }

    public Integer getIncomingUnits() {
        return incomingUnits;
    }

    public void setIncomingUnits(Integer incomingUnits) {
        this.incomingUnits = incomingUnits;
    }
}

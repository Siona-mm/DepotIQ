package com.depotiq.dtos.inventory;

import java.time.LocalDateTime;

public class DepotInventoryResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String category;
    private Integer availableUnits;
    private Integer reservedUnits;
    private Integer freeUnits;
    private LocalDateTime lastUpdated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Integer getFreeUnits() {
        return freeUnits;
    }

    public void setFreeUnits(Integer freeUnits) {
        this.freeUnits = freeUnits;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

package com.depotiq.dtos.store;

import com.depotiq.models.StoreType;

import java.time.LocalDateTime;

public class StoreResponse {
    private Long id;
    private String storeCode;
    private String name;
    private StoreType storeType;
    private String region;
    private Boolean hasWarehouse;
    private Integer storageCapacity;
    private Integer deliveryLeadTimeDays;
    private Integer preferredHorizonDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public void setStoreType(StoreType storeType) {
        this.storeType = storeType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getHasWarehouse() {
        return hasWarehouse;
    }

    public void setHasWarehouse(Boolean hasWarehouse) {
        this.hasWarehouse = hasWarehouse;
    }

    public Integer getStorageCapacity() {
        return storageCapacity;
    }

    public void setStorageCapacity(Integer storageCapacity) {
        this.storageCapacity = storageCapacity;
    }

    public Integer getDeliveryLeadTimeDays() {
        return deliveryLeadTimeDays;
    }

    public void setDeliveryLeadTimeDays(Integer deliveryLeadTimeDays) {
        this.deliveryLeadTimeDays = deliveryLeadTimeDays;
    }

    public Integer getPreferredHorizonDays() {
        return preferredHorizonDays;
    }

    public void setPreferredHorizonDays(Integer preferredHorizonDays) {
        this.preferredHorizonDays = preferredHorizonDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

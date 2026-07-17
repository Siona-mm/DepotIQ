package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

    @Column(name = "store_code", nullable = false, unique = true, length = 50)
    private String storeCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", nullable = false, length = 50)
    private StoreType storeType;

    @Column(length = 100)
    private String region;

    @Column(name = "has_warehouse", nullable = false)
    private Boolean hasWarehouse = false;

    @Column(name = "storage_capacity", nullable = false)
    private Integer storageCapacity = 0;

    @Column(name = "delivery_lead_time_days", nullable = false)
    private Integer deliveryLeadTimeDays = 0;

    @Column(name = "preferred_horizon_days", nullable = false)
    private Integer preferredHorizonDays = 7;

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
}

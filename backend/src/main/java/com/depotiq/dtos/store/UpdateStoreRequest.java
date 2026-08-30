package com.depotiq.dtos.store;

import com.depotiq.models.StoreType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateStoreRequest {
    @Size(max = 100)
    private String externalStoreId;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private StoreType storeType;

    @NotBlank
    @Size(max = 100)
    private String region;

    @NotNull
    private Boolean hasWarehouse;

    @NotNull
    @Min(1)
    private Integer storageCapacity;

    @NotNull
    @Min(1)
    private Integer deliveryLeadTimeDays;

    @NotNull
    @Min(1)
    private Integer preferredHorizonDays;

    @AssertTrue(message = "Preferred horizon must be 3, 7, 14, or 30 days")
    public boolean isPreferredHorizonSupported() {
        return preferredHorizonDays == null || java.util.List.of(3, 7, 14, 30).contains(preferredHorizonDays);
    }

    public String getExternalStoreId() {
        return externalStoreId;
    }

    public void setExternalStoreId(String externalStoreId) {
        this.externalStoreId = externalStoreId;
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

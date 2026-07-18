package com.depotiq.mappers;

import com.depotiq.dtos.store.CreateStoreRequest;
import com.depotiq.dtos.store.StoreResponse;
import com.depotiq.dtos.store.UpdateStoreRequest;
import com.depotiq.models.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {
    public StoreResponse toResponse(Store store) {
        StoreResponse response = new StoreResponse();

        response.setId(store.getId());
        response.setStoreCode(store.getStoreCode());
        response.setName(store.getName());
        response.setStoreType(store.getStoreType());
        response.setRegion(store.getRegion());
        response.setHasWarehouse(store.getHasWarehouse());
        response.setStorageCapacity(store.getStorageCapacity());
        response.setDeliveryLeadTimeDays(store.getDeliveryLeadTimeDays());
        response.setPreferredHorizonDays(store.getPreferredHorizonDays());
        response.setCreatedAt(store.getCreatedAt());
        response.setUpdatedAt(store.getUpdatedAt());

        return response;
    }

    public Store toEntity(CreateStoreRequest request) {
        Store store = new Store();

        store.setStoreCode(request.getStoreCode());
        store.setName(request.getName());
        store.setStoreType(request.getStoreType());
        store.setRegion(request.getRegion());
        store.setHasWarehouse(request.getHasWarehouse());
        store.setStorageCapacity(request.getStorageCapacity());
        store.setDeliveryLeadTimeDays(request.getDeliveryLeadTimeDays());
        store.setPreferredHorizonDays(request.getPreferredHorizonDays());

        return store;
    }

    public void updateEntity(Store store, UpdateStoreRequest request) {
        store.setName(request.getName());
        store.setStoreType(request.getStoreType());
        store.setRegion(request.getRegion());
        store.setHasWarehouse(request.getHasWarehouse());
        store.setStorageCapacity(request.getStorageCapacity());
        store.setDeliveryLeadTimeDays(request.getDeliveryLeadTimeDays());
        store.setPreferredHorizonDays(request.getPreferredHorizonDays());
    }
}

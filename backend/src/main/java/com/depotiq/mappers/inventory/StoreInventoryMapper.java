package com.depotiq.mappers.inventory;

import com.depotiq.dtos.inventory.store.StoreInventoryRequest;
import com.depotiq.dtos.inventory.store.StoreInventoryResponse;
import com.depotiq.models.inventory.StoreInventory;
import org.springframework.stereotype.Component;

@Component
public class StoreInventoryMapper {

    public StoreInventory toEntity(StoreInventoryRequest request) {
        StoreInventory inventory = new StoreInventory();
        updateEntity(request, inventory);
        return inventory;
    }

    public void updateEntity(StoreInventoryRequest request, StoreInventory inventory) {
        inventory.setStoreId(request.storeId());
        inventory.setProductId(request.productId());
        inventory.setInventoryLevel(request.inventoryLevel());
        inventory.setReorderPoint(request.reorderPoint());
    }

    public StoreInventoryResponse toResponse(StoreInventory inventory) {
        return new StoreInventoryResponse(
                inventory.getId(),
                inventory.getStoreId(),
                inventory.getProductId(),
                inventory.getInventoryLevel(),
                inventory.getReorderPoint(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}

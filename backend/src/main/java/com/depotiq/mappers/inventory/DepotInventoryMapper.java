package com.depotiq.mappers.inventory;

import com.depotiq.dtos.inventory.depot.DepotInventoryRequest;
import com.depotiq.dtos.inventory.depot.DepotInventoryResponse;
import com.depotiq.models.inventory.DepotInventory;
import org.springframework.stereotype.Component;

@Component
public class DepotInventoryMapper {

    public DepotInventory toEntity(DepotInventoryRequest request) {
        DepotInventory inventory = new DepotInventory();
        updateEntity(request, inventory);
        return inventory;
    }

    public void updateEntity(DepotInventoryRequest request, DepotInventory inventory) {
        inventory.setDepotId(request.depotId());
        inventory.setProductId(request.productId());
        inventory.setInventoryLevel(request.inventoryLevel());
        inventory.setReorderPoint(request.reorderPoint());
    }

    public DepotInventoryResponse toResponse(DepotInventory inventory) {
        return new DepotInventoryResponse(
                inventory.getId(),
                inventory.getDepotId(),
                inventory.getProductId(),
                inventory.getInventoryLevel(),
                inventory.getReorderPoint(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}

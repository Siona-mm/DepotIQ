package com.depotiq.mappers;

import com.depotiq.dtos.inventory.DepotInventoryResponse;
import com.depotiq.dtos.inventory.StoreInventoryResponse;
import com.depotiq.models.DepotInventory;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.models.StoreInventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public StoreInventoryResponse toStoreInventoryResponse(StoreInventory inventory) {
        Store store = inventory.getStore();
        Product product = inventory.getProduct();
        StoreInventoryResponse response = new StoreInventoryResponse();

        response.setId(inventory.getId());
        response.setStoreId(store.getId());
        response.setStoreCode(store.getStoreCode());
        response.setStoreName(store.getName());
        response.setProductId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getName());
        response.setCategory(product.getCategory());
        response.setInventoryLevel(inventory.getInventoryLevel());
        response.setIncomingUnits(inventory.getIncomingUnits());
        response.setLastUpdated(inventory.getLastUpdated());

        return response;
    }

    public DepotInventoryResponse toDepotInventoryResponse(DepotInventory inventory) {
        Product product = inventory.getProduct();
        DepotInventoryResponse response = new DepotInventoryResponse();

        response.setId(inventory.getId());
        response.setProductId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getName());
        response.setCategory(product.getCategory());
        response.setAvailableUnits(inventory.getAvailableUnits());
        response.setReservedUnits(inventory.getReservedUnits());
        response.setFreeUnits(inventory.getAvailableUnits() - inventory.getReservedUnits());
        response.setLastUpdated(inventory.getLastUpdated());

        return response;
    }
}

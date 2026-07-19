package com.depotiq.mappers.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.depotiq.dtos.inventory.store.StoreInventoryRequest;
import com.depotiq.dtos.inventory.store.StoreInventoryResponse;
import com.depotiq.models.inventory.StoreInventory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StoreInventoryMapperTest {

    private final StoreInventoryMapper mapper = new StoreInventoryMapper();

    @Test
    void mapsRequestToEntity() {
        StoreInventory inventory = mapper.toEntity(new StoreInventoryRequest("S001", "P0001", 231, 50));

        assertThat(inventory.getStoreId()).isEqualTo("S001");
        assertThat(inventory.getProductId()).isEqualTo("P0001");
        assertThat(inventory.getInventoryLevel()).isEqualTo(231);
        assertThat(inventory.getReorderPoint()).isEqualTo(50);
    }

    @Test
    void mapsEntityToResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 19, 10, 0);
        StoreInventory inventory = new StoreInventory();
        inventory.setId(1L);
        inventory.setStoreId("S001");
        inventory.setProductId("P0001");
        inventory.setInventoryLevel(231);
        inventory.setReorderPoint(50);
        inventory.setCreatedAt(createdAt);
        inventory.setUpdatedAt(createdAt);

        StoreInventoryResponse response = mapper.toResponse(inventory);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.inventoryLevel()).isEqualTo(231);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}

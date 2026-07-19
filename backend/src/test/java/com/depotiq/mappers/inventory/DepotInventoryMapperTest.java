package com.depotiq.mappers.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.depotiq.dtos.inventory.depot.DepotInventoryRequest;
import com.depotiq.dtos.inventory.depot.DepotInventoryResponse;
import com.depotiq.models.inventory.DepotInventory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DepotInventoryMapperTest {

    private final DepotInventoryMapper mapper = new DepotInventoryMapper();

    @Test
    void mapsRequestToEntity() {
        DepotInventory inventory = mapper.toEntity(new DepotInventoryRequest("D001", "P0001", 1000, 200));

        assertThat(inventory.getDepotId()).isEqualTo("D001");
        assertThat(inventory.getProductId()).isEqualTo("P0001");
        assertThat(inventory.getInventoryLevel()).isEqualTo(1000);
        assertThat(inventory.getReorderPoint()).isEqualTo(200);
    }

    @Test
    void mapsEntityToResponse() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 19, 10, 0);
        DepotInventory inventory = new DepotInventory();
        inventory.setId(1L);
        inventory.setDepotId("D001");
        inventory.setProductId("P0001");
        inventory.setInventoryLevel(1000);
        inventory.setReorderPoint(200);
        inventory.setCreatedAt(updatedAt);
        inventory.setUpdatedAt(updatedAt);

        DepotInventoryResponse response = mapper.toResponse(inventory);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.depotId()).isEqualTo("D001");
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}

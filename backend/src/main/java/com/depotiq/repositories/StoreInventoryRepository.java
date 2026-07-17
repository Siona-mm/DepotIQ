package com.depotiq.repositories;

import com.depotiq.models.StoreInventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    List<StoreInventory> findByStoreId(Long storeId);

    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);
}

package com.depotiq.repositories;

import com.depotiq.models.StoreInventory;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    List<StoreInventory> findByStoreId(Long storeId);

    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from StoreInventory inventory "
            + "where inventory.store.id = :storeId "
            + "and inventory.product.id = :productId")
    Optional<StoreInventory> findByStoreIdAndProductIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId
    );
}

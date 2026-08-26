package com.depotiq.repositories;

import com.depotiq.models.DepotInventory;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepotInventoryRepository extends JpaRepository<DepotInventory, Long> {
    Optional<DepotInventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from DepotInventory inventory "
            + "where inventory.product.id = :productId")
    Optional<DepotInventory> findByProductIdForUpdate(
            @Param("productId") Long productId
    );
}

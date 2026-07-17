package com.depotiq.repositories;

import com.depotiq.models.DepotInventory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepotInventoryRepository extends JpaRepository<DepotInventory, Long> {
    Optional<DepotInventory> findByProductId(Long productId);
}

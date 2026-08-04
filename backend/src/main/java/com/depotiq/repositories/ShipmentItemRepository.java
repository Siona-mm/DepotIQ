package com.depotiq.repositories;

import com.depotiq.models.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    boolean existsByRecommendationId(Long recommendationId);
}

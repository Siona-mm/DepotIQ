package com.depotiq.repositories;

import com.depotiq.models.RecommendationPriority;
import com.depotiq.models.RecommendationStatus;

import com.depotiq.models.ShipmentRecommendation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRecommendationRepository
    extends JpaRepository<ShipmentRecommendation, Long> {

    List<ShipmentRecommendation> findByPriority(RecommendationPriority priority);

    List<ShipmentRecommendation> findByStatus(RecommendationStatus status);

    List<ShipmentRecommendation> findByStoreId(Long storeId);
}

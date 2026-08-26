package com.depotiq.repositories;

import com.depotiq.models.RecommendationPriority;
import com.depotiq.models.RecommendationStatus;

import com.depotiq.models.ShipmentRecommendation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ShipmentRecommendationRepository
    extends JpaRepository<ShipmentRecommendation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select recommendation from ShipmentRecommendation recommendation "
            + "join fetch recommendation.store "
            + "join fetch recommendation.product "
            + "where recommendation.id in :ids")
    List<ShipmentRecommendation> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    List<ShipmentRecommendation> findByPriority(RecommendationPriority priority);

    List<ShipmentRecommendation> findByStatus(RecommendationStatus status);

    List<ShipmentRecommendation> findByStoreId(Long storeId);

    Optional<ShipmentRecommendation>
        findByStoreIdAndProductIdAndRecommendationDateAndHorizonDays(
            Long storeId,
            Long productId,
            LocalDate recommendationDate,
            Integer horizonDays
        );
}

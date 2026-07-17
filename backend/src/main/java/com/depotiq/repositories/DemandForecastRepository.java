package com.depotiq.repositories;

import com.depotiq.models.DemandForecast;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByStoreId(Long storeId);

    Optional<DemandForecast> findByStoreIdAndProductIdAndForecastDateAndHorizonDays(
        Long storeId,
        Long productId,
        LocalDate forecastDate,
        Integer horizonDays
    );
}

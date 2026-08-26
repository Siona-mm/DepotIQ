package com.depotiq.services;

import com.depotiq.dtos.ml.MlHealthResponse;
import com.depotiq.dtos.ml.MlModelInfoResponse;
import com.depotiq.dtos.ml.MlStatusResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MlStatusService {
    private final MlServiceClient mlServiceClient;
    private final DemandForecastRepository demandForecastRepository;
    private final ShipmentRecommendationRepository recommendationRepository;

    public MlStatusService(
            MlServiceClient mlServiceClient,
            DemandForecastRepository demandForecastRepository,
            ShipmentRecommendationRepository recommendationRepository
    ) {
        this.mlServiceClient = mlServiceClient;
        this.demandForecastRepository = demandForecastRepository;
        this.recommendationRepository = recommendationRepository;
    }

    public MlStatusResponse getStatus() {
        MlHealthResponse health = mlServiceClient.getHealth();
        List<MlModelInfoResponse> models = mlServiceClient.getModels();
        List<DemandForecast> forecasts = demandForecastRepository.findAll();

        return new MlStatusResponse(
                "ok".equalsIgnoreCase(health.status()),
                health.status(),
                models,
                forecasts.size(),
                recommendationRepository.count(),
                distinctStoreCount(forecasts),
                distinctProductCount(forecasts),
                averageMae(forecasts),
                latestForecastDate(forecasts),
                lastSynchronizedAt(forecasts)
        );
    }

    private long distinctStoreCount(List<DemandForecast> forecasts) {
        return forecasts.stream()
                .map(DemandForecast::getStore)
                .filter(Objects::nonNull)
                .map(store -> store.getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long distinctProductCount(List<DemandForecast> forecasts) {
        return forecasts.stream()
                .map(DemandForecast::getProduct)
                .filter(Objects::nonNull)
                .map(product -> product.getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private BigDecimal averageMae(List<DemandForecast> forecasts) {
        List<BigDecimal> values = forecasts.stream()
                .map(DemandForecast::getModelMae)
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return null;
        }

        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private LocalDate latestForecastDate(List<DemandForecast> forecasts) {
        return forecasts.stream()
                .map(DemandForecast::getForecastDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private LocalDateTime lastSynchronizedAt(List<DemandForecast> forecasts) {
        return forecasts.stream()
                .map(DemandForecast::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}

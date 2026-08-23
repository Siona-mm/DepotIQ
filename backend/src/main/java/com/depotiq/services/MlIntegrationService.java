package com.depotiq.services;

import com.depotiq.dtos.ml.MlRecommendationBatchResponse;
import com.depotiq.dtos.ml.MlDataSyncRequest;
import com.depotiq.dtos.ml.MlDataSyncResponse;
import com.depotiq.dtos.ml.MlSalesRecordPayload;
import com.depotiq.dtos.ml.MlStoreInventoryPayload;
import com.depotiq.dtos.ml.MlRecommendationPayload;
import com.depotiq.dtos.ml.MlSyncResponse;
import com.depotiq.dtos.ml.MlStatusResponse;
import com.depotiq.dtos.ml.MlModelInfoResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.RecommendationPriority;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.models.Store;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import com.depotiq.repositories.StoreRepository;
import com.depotiq.repositories.SalesRecordRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MlIntegrationService {
    private final MlServiceClient mlServiceClient;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final ShipmentRecommendationRepository recommendationRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    public MlIntegrationService(
            MlServiceClient mlServiceClient,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            DemandForecastRepository demandForecastRepository,
            ShipmentRecommendationRepository recommendationRepository,
            SalesRecordRepository salesRecordRepository,
            StoreInventoryRepository storeInventoryRepository
    ) {
        this.mlServiceClient = mlServiceClient;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.demandForecastRepository = demandForecastRepository;
        this.recommendationRepository = recommendationRepository;
        this.salesRecordRepository = salesRecordRepository;
        this.storeInventoryRepository = storeInventoryRepository;
    }

    public MlSyncResponse syncRecommendations() {
        MlRecommendationBatchResponse batch = mlServiceClient.getRecommendations();
        int forecastsSynced = 0;
        int recommendationsSynced = 0;
        int skipped = 0;

        for (MlRecommendationPayload payload : batch.recommendations()) {
            Optional<Store> store = storeRepository.findByStoreCode(payload.storeCode());
            Optional<Product> product = productRepository.findByProductCode(payload.productCode());

            if (store.isEmpty() || product.isEmpty()) {
                skipped++;
                continue;
            }

            DemandForecast forecast = upsertForecast(payload, store.get(), product.get());
            upsertRecommendation(payload, store.get(), product.get(), forecast);
            forecastsSynced++;
            recommendationsSynced++;
        }

        return new MlSyncResponse(
                batch.sourceDate(),
                batch.recommendations().size(),
                forecastsSynced,
                recommendationsSynced,
                skipped
        );
    }

    public MlDataSyncResponse syncImportedData() {
        MlDataSyncRequest request = new MlDataSyncRequest(
                LocalDateTime.now(),
                salesRecordRepository.findAll().stream()
                        .sorted(Comparator.comparing(record -> record.getSaleDate()))
                        .map(record -> new MlSalesRecordPayload(
                                record.getStore().getStoreCode(),
                                record.getProduct().getProductCode(),
                                record.getSaleDate(),
                                record.getUnitsSold(),
                                record.getPrice(),
                                record.getDiscount(),
                                record.getPromotion(),
                                record.getWeatherCondition(),
                                record.getHolidayPromotion(),
                                record.getSeasonality()
                        ))
                        .toList(),
                storeInventoryRepository.findAll().stream()
                        .map(inventory -> new MlStoreInventoryPayload(
                                inventory.getStore().getStoreCode(),
                                inventory.getProduct().getProductCode(),
                                inventory.getInventoryLevel(),
                                inventory.getIncomingUnits()
                        ))
                        .toList()
        );
        return mlServiceClient.syncData(request);
    }

    @Transactional(readOnly = true)
    public MlStatusResponse getStatus() {
        List<DemandForecast> forecasts = demandForecastRepository.findAll();
        List<ShipmentRecommendation> recommendations = recommendationRepository.findAll();

        boolean serviceAvailable = true;
        String serviceStatus = "ok";
        List<MlModelInfoResponse> models = List.of();
        try {
            serviceStatus = mlServiceClient.getHealth().status();
            models = mlServiceClient.getModels();
        } catch (ResponseStatusException exception) {
            serviceAvailable = false;
            serviceStatus = "unavailable";
        }

        return new MlStatusResponse(
                serviceAvailable,
                serviceStatus,
                models,
                forecasts.size(),
                recommendations.size(),
                forecasts.stream().map(forecast -> forecast.getStore().getId()).distinct().count(),
                forecasts.stream().map(forecast -> forecast.getProduct().getId()).distinct().count(),
                averageMae(forecasts),
                forecasts.stream()
                        .map(DemandForecast::getForecastDate)
                        .max(Comparator.naturalOrder())
                        .orElse(null),
                forecasts.stream()
                        .map(DemandForecast::getUpdatedAt)
                        .filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null)
        );
    }

    private BigDecimal averageMae(List<DemandForecast> forecasts) {
        List<BigDecimal> values = forecasts.stream()
                .map(DemandForecast::getModelMae)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return null;
        }

        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    private DemandForecast upsertForecast(
            MlRecommendationPayload payload,
            Store store,
            Product product
    ) {
        DemandForecast forecast = demandForecastRepository
                .findByStoreIdAndProductIdAndForecastDateAndHorizonDays(
                        store.getId(),
                        product.getId(),
                        payload.forecastDate(),
                        payload.horizonDays()
                )
                .orElseGet(DemandForecast::new);

        forecast.setStore(store);
        forecast.setProduct(product);
        forecast.setForecastDate(payload.forecastDate());
        forecast.setHorizonDays(payload.horizonDays());
        forecast.setPredictedDemand(payload.predictedDemand());
        forecast.setConfidenceLower(payload.confidenceLower());
        forecast.setConfidenceUpper(payload.confidenceUpper());
        forecast.setModelName(payload.modelName());
        forecast.setModelVersion(payload.modelVersion());
        forecast.setModelMae(payload.modelMae());

        return demandForecastRepository.save(forecast);
    }

    private void upsertRecommendation(
            MlRecommendationPayload payload,
            Store store,
            Product product,
            DemandForecast forecast
    ) {
        ShipmentRecommendation recommendation = recommendationRepository
                .findByStoreIdAndProductIdAndRecommendationDateAndHorizonDays(
                        store.getId(),
                        product.getId(),
                        payload.forecastDate(),
                        payload.horizonDays()
                )
                .orElseGet(ShipmentRecommendation::new);

        recommendation.setStore(store);
        recommendation.setProduct(product);
        recommendation.setDemandForecast(forecast);
        recommendation.setRecommendationDate(payload.forecastDate());
        recommendation.setHorizonDays(payload.horizonDays());
        recommendation.setPredictedDemand(payload.predictedDemand());
        recommendation.setConfidenceLower(payload.confidenceLower());
        recommendation.setConfidenceUpper(payload.confidenceUpper());
        recommendation.setCurrentInventory(payload.currentInventory());
        recommendation.setIncomingUnits(payload.incomingUnits());
        recommendation.setSafetyStock(payload.safetyStock());
        recommendation.setRequiredStock(payload.requiredStock());
        recommendation.setRecommendedShipment(payload.recommendedShipment());
        recommendation.setPriority(parsePriority(payload.priority()));
        recommendation.setExplanation(payload.explanation());

        if (recommendation.getId() == null) {
            recommendation.setStatus(RecommendationStatus.PENDING);
        }

        recommendationRepository.save(recommendation);
    }

    private RecommendationPriority parsePriority(String priority) {
        return RecommendationPriority.valueOf(
                priority.trim().toUpperCase(Locale.ROOT).replace(' ', '_')
        );
    }
}

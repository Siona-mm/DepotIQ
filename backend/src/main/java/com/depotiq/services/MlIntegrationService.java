package com.depotiq.services;

import com.depotiq.dtos.ml.MlRecommendationBatchResponse;
import com.depotiq.dtos.ml.MlDataSyncRequest;
import com.depotiq.dtos.ml.MlDataSyncResponse;
import com.depotiq.dtos.ml.MlSalesRecordPayload;
import com.depotiq.dtos.ml.MlStoreInventoryPayload;
import com.depotiq.dtos.ml.MlRecommendationPayload;
import com.depotiq.dtos.ml.MlSyncResponse;
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
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MlIntegrationService {
    private static final int MODEL_HISTORY_DAYS = 60;

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
            Optional<Store> store = storeRepository.findByStoreCode(BusinessCodes.normalizeStoreCode(payload.storeCode()));
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
        var recentSalesRecords = salesRecordRepository.findTopByOrderBySaleDateDesc()
                .map(latest -> salesRecordRepository
                        .findBySaleDateGreaterThanEqualOrderBySaleDateAsc(
                                latest.getSaleDate().minusDays(MODEL_HISTORY_DAYS)
                        ))
                .orElseGet(java.util.List::of);
        MlDataSyncRequest request = new MlDataSyncRequest(
                LocalDateTime.now(),
                recentSalesRecords.stream()
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
        boolean isNewRecommendation = recommendation.getId() == null;
        boolean canRefreshSuggestedShipment = isNewRecommendation
                || recommendation.getStatus() == RecommendationStatus.PENDING;

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
        recommendation.setExplanation(payload.explanation());

        if (canRefreshSuggestedShipment) {
            recommendation.setRecommendedShipment(payload.recommendedShipment());
            recommendation.setPriority(parsePriority(payload.priority()));
        }

        if (isNewRecommendation) {
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

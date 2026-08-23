package com.depotiq.services;

import com.depotiq.dtos.recommendation.CreateShipmentRecommendationRequest;
import com.depotiq.dtos.recommendation.OverrideRecommendationRequest;
import com.depotiq.dtos.recommendation.ShipmentRecommendationResponse;
import com.depotiq.dtos.recommendation.UpdateRecommendationStatusRequest;
import com.depotiq.mappers.ShipmentRecommendationMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.OffsetDateTime;

@Service
@Transactional
public class ShipmentRecommendationService {
    private final ShipmentRecommendationRepository shipmentRecommendationRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final ShipmentRecommendationMapper shipmentRecommendationMapper;
    private final UserSettingsService userSettingsService;
    private final OperationalActivityService operationalActivityService;

    public ShipmentRecommendationService(
            ShipmentRecommendationRepository shipmentRecommendationRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            DemandForecastRepository demandForecastRepository,
            ShipmentRecommendationMapper shipmentRecommendationMapper
    ) {
        this(shipmentRecommendationRepository, storeRepository, productRepository,
                demandForecastRepository, shipmentRecommendationMapper, null, null);
    }

    public ShipmentRecommendationService(
            ShipmentRecommendationRepository shipmentRecommendationRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            DemandForecastRepository demandForecastRepository,
            ShipmentRecommendationMapper shipmentRecommendationMapper,
            UserSettingsService userSettingsService
    ) {
        this(shipmentRecommendationRepository, storeRepository, productRepository,
                demandForecastRepository, shipmentRecommendationMapper, userSettingsService, null);
    }

    @Autowired
    public ShipmentRecommendationService(
            ShipmentRecommendationRepository shipmentRecommendationRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            DemandForecastRepository demandForecastRepository,
            ShipmentRecommendationMapper shipmentRecommendationMapper,
            UserSettingsService userSettingsService,
            OperationalActivityService operationalActivityService
    ) {
        this.shipmentRecommendationRepository = shipmentRecommendationRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.demandForecastRepository = demandForecastRepository;
        this.shipmentRecommendationMapper = shipmentRecommendationMapper;
        this.userSettingsService = userSettingsService;
        this.operationalActivityService = operationalActivityService;
    }

    public List<ShipmentRecommendationResponse> getAllRecommendations() {
        return shipmentRecommendationRepository.findAll()
                .stream()
                .map(shipmentRecommendationMapper::toResponse)
                .toList();
    }

    public ShipmentRecommendationResponse getRecommendationById(Long id) {
        return shipmentRecommendationMapper.toResponse(findRecommendationOrThrow(id));
    }

    public List<ShipmentRecommendationResponse> getRecommendationsByStore(Long storeId) {
        return shipmentRecommendationRepository.findByStoreId(storeId)
                .stream()
                .map(shipmentRecommendationMapper::toResponse)
                .toList();
    }

    public List<ShipmentRecommendationResponse> getRecommendationsByPriority(RecommendationPriority priority) {
        return shipmentRecommendationRepository.findByPriority(priority)
                .stream()
                .map(shipmentRecommendationMapper::toResponse)
                .toList();
    }

    public List<ShipmentRecommendationResponse> getRecommendationsByStatus(RecommendationStatus status) {
        return shipmentRecommendationRepository.findByStatus(status)
                .stream()
                .map(shipmentRecommendationMapper::toResponse)
                .toList();
    }

    public ShipmentRecommendationResponse createRecommendation(CreateShipmentRecommendationRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        DemandForecast forecast = null;

        if (request.getDemandForecastId() != null) {
            forecast = demandForecastRepository.findById(request.getDemandForecastId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Forecast not found"));
        }

        ShipmentRecommendation recommendation = shipmentRecommendationMapper.toEntity(request, store, product, forecast);
        ShipmentRecommendation saved = shipmentRecommendationRepository.save(recommendation);
        if (operationalActivityService != null) {
            operationalActivityService.record("RECOMMENDATION_OVERRIDDEN", username,
                    "RECOMMENDATION", saved.getId(),
                    saved.getStore().getStoreCode() + " / " + saved.getProduct().getProductCode(),
                    request.getReason().trim());
        }
        return shipmentRecommendationMapper.toResponse(saved);
    }

    public ShipmentRecommendationResponse updateRecommendationStatus(
            Long id,
            UpdateRecommendationStatusRequest request
    ) {
        ShipmentRecommendation recommendation = findRecommendationOrThrow(id);
        recommendation.setStatus(request.getStatus());

        return shipmentRecommendationMapper.toResponse(shipmentRecommendationRepository.save(recommendation));
    }

    public ShipmentRecommendationResponse overrideRecommendedShipment(
            Long id,
            OverrideRecommendationRequest request
    ) {
        return overrideRecommendedShipment(id, request, null);
    }

    public ShipmentRecommendationResponse overrideRecommendedShipment(
            Long id,
            OverrideRecommendationRequest request,
            String username
    ) {
        if (username != null && userSettingsService != null
                && !userSettingsService.allowsRecommendationOverrides(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recommendation overrides are disabled in your settings"
            );
        }
        ShipmentRecommendation recommendation = findRecommendationOrThrow(id);

        if (recommendation.getStatus() != RecommendationStatus.PENDING
                && recommendation.getStatus() != RecommendationStatus.EDITED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending or edited recommendations can be overridden"
            );
        }

        if (recommendation.getOriginalRecommendedShipment() == null) {
            recommendation.setOriginalRecommendedShipment(recommendation.getRecommendedShipment());
        }

        recommendation.setRecommendedShipment(request.getRecommendedShipment());
        recommendation.setOverrideReason(request.getReason().trim());
        recommendation.setOverriddenBy(request.getOverriddenBy().trim());
        recommendation.setOverriddenAt(OffsetDateTime.now());
        recommendation.setStatus(RecommendationStatus.EDITED);

        return shipmentRecommendationMapper.toResponse(shipmentRecommendationRepository.save(recommendation));
    }

    public void deleteRecommendation(Long id) {
        ShipmentRecommendation recommendation = findRecommendationOrThrow(id);
        shipmentRecommendationRepository.delete(recommendation);
    }

    private ShipmentRecommendation findRecommendationOrThrow(Long id) {
        return shipmentRecommendationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found"));
    }
}

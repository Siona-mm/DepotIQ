package com.depotiq.mappers;

import com.depotiq.dtos.recommendation.CreateShipmentRecommendationRequest;
import com.depotiq.dtos.recommendation.ShipmentRecommendationResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.models.Store;
import org.springframework.stereotype.Component;

@Component
public class ShipmentRecommendationMapper {
    public ShipmentRecommendationResponse toResponse(ShipmentRecommendation recommendation) {
        Store store = recommendation.getStore();
        Product product = recommendation.getProduct();
        DemandForecast forecast = recommendation.getDemandForecast();
        ShipmentRecommendationResponse response = new ShipmentRecommendationResponse();

        response.setId(recommendation.getId());
        response.setStoreId(store.getId());
        response.setStoreCode(store.getStoreCode());
        response.setStoreName(store.getName());
        response.setProductId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getName());
        response.setCategory(product.getCategory());
        response.setDemandForecastId(forecast == null ? null : forecast.getId());
        response.setRecommendationDate(recommendation.getRecommendationDate());
        response.setHorizonDays(recommendation.getHorizonDays());
        response.setPredictedDemand(recommendation.getPredictedDemand());
        response.setConfidenceLower(recommendation.getConfidenceLower());
        response.setConfidenceUpper(recommendation.getConfidenceUpper());
        response.setCurrentInventory(recommendation.getCurrentInventory());
        response.setIncomingUnits(recommendation.getIncomingUnits());
        response.setSafetyStock(recommendation.getSafetyStock());
        response.setRequiredStock(recommendation.getRequiredStock());
        response.setRecommendedShipment(recommendation.getRecommendedShipment());
        response.setOriginalRecommendedShipment(recommendation.getOriginalRecommendedShipment());
        response.setOverrideReason(recommendation.getOverrideReason());
        response.setOverriddenBy(recommendation.getOverriddenBy());
        response.setOverriddenAt(recommendation.getOverriddenAt());
        response.setPriority(recommendation.getPriority());
        response.setStatus(recommendation.getStatus());
        response.setExplanation(recommendation.getExplanation());

        return response;
    }

    public ShipmentRecommendation toEntity(
            CreateShipmentRecommendationRequest request,
            Store store,
            Product product,
            DemandForecast forecast
    ) {
        ShipmentRecommendation recommendation = new ShipmentRecommendation();

        recommendation.setStore(store);
        recommendation.setProduct(product);
        recommendation.setDemandForecast(forecast);
        recommendation.setRecommendationDate(request.getRecommendationDate());
        recommendation.setHorizonDays(request.getHorizonDays());
        recommendation.setPredictedDemand(request.getPredictedDemand());
        recommendation.setConfidenceLower(request.getConfidenceLower());
        recommendation.setConfidenceUpper(request.getConfidenceUpper());
        recommendation.setCurrentInventory(request.getCurrentInventory());
        recommendation.setIncomingUnits(request.getIncomingUnits());
        recommendation.setSafetyStock(request.getSafetyStock());
        recommendation.setRequiredStock(request.getRequiredStock());
        recommendation.setRecommendedShipment(request.getRecommendedShipment());
        recommendation.setPriority(request.getPriority());
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setExplanation(request.getExplanation());

        return recommendation;
    }
}

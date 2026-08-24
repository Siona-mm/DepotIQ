package com.depotiq.controllers;

import com.depotiq.dtos.recommendation.CreateShipmentRecommendationRequest;
import com.depotiq.dtos.recommendation.OverrideRecommendationRequest;
import com.depotiq.dtos.recommendation.ShipmentRecommendationResponse;
import com.depotiq.dtos.recommendation.UpdateRecommendationStatusRequest;
import com.depotiq.models.RecommendationPriority;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.services.ShipmentRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class ShipmentRecommendationController {
    private final ShipmentRecommendationService shipmentRecommendationService;

    public ShipmentRecommendationController(ShipmentRecommendationService shipmentRecommendationService) {
        this.shipmentRecommendationService = shipmentRecommendationService;
    }

    @GetMapping
    public List<ShipmentRecommendationResponse> getRecommendations(
            @RequestParam(required = false) RecommendationPriority priority,
            @RequestParam(required = false) RecommendationStatus status
    ) {
        if (priority != null) {
            return shipmentRecommendationService.getRecommendationsByPriority(priority);
        }

        if (status != null) {
            return shipmentRecommendationService.getRecommendationsByStatus(status);
        }

        return shipmentRecommendationService.getAllRecommendations();
    }

    @GetMapping("/{id}")
    public ShipmentRecommendationResponse getRecommendationById(@PathVariable Long id) {
        return shipmentRecommendationService.getRecommendationById(id);
    }

    @GetMapping("/stores/{storeId}")
    public List<ShipmentRecommendationResponse> getRecommendationsByStore(@PathVariable Long storeId) {
        return shipmentRecommendationService.getRecommendationsByStore(storeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentRecommendationResponse createRecommendation(
            @Valid @RequestBody CreateShipmentRecommendationRequest request
    ) {
        return shipmentRecommendationService.createRecommendation(request);
    }

    @PatchMapping("/{id}/status")
    public ShipmentRecommendationResponse updateRecommendationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecommendationStatusRequest request
    ) {
        return shipmentRecommendationService.updateRecommendationStatus(id, request);
    }

    @PatchMapping("/{id}/override")
    public ShipmentRecommendationResponse overrideRecommendedShipment(
            @PathVariable Long id,
            @Valid @RequestBody OverrideRecommendationRequest request
    ) {
        return shipmentRecommendationService.overrideRecommendedShipment(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecommendation(@PathVariable Long id) {
        shipmentRecommendationService.deleteRecommendation(id);
    }
}

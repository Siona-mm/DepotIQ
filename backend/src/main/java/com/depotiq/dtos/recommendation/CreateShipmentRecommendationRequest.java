package com.depotiq.dtos.recommendation;

import com.depotiq.models.RecommendationPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateShipmentRecommendationRequest {
    @NotNull
    private Long storeId;

    @NotNull
    private Long productId;

    private Long demandForecastId;

    @NotNull
    private LocalDate recommendationDate;

    @NotNull
    @Min(1)
    private Integer horizonDays;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal predictedDemand;

    @DecimalMin("0.00")
    private BigDecimal confidenceLower;

    @DecimalMin("0.00")
    private BigDecimal confidenceUpper;

    @NotNull
    @Min(0)
    private Integer currentInventory;

    @NotNull
    @Min(0)
    private Integer incomingUnits;

    @NotNull
    @Min(0)
    private Integer safetyStock;

    @NotNull
    @Min(0)
    private Integer requiredStock;

    @NotNull
    @Min(0)
    private Integer recommendedShipment;

    @NotNull
    private RecommendationPriority priority;

    private String explanation;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getDemandForecastId() {
        return demandForecastId;
    }

    public void setDemandForecastId(Long demandForecastId) {
        this.demandForecastId = demandForecastId;
    }

    public LocalDate getRecommendationDate() {
        return recommendationDate;
    }

    public void setRecommendationDate(LocalDate recommendationDate) {
        this.recommendationDate = recommendationDate;
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }

    public void setHorizonDays(Integer horizonDays) {
        this.horizonDays = horizonDays;
    }

    public BigDecimal getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(BigDecimal predictedDemand) {
        this.predictedDemand = predictedDemand;
    }

    public BigDecimal getConfidenceLower() {
        return confidenceLower;
    }

    public void setConfidenceLower(BigDecimal confidenceLower) {
        this.confidenceLower = confidenceLower;
    }

    public BigDecimal getConfidenceUpper() {
        return confidenceUpper;
    }

    public void setConfidenceUpper(BigDecimal confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }

    public Integer getCurrentInventory() {
        return currentInventory;
    }

    public void setCurrentInventory(Integer currentInventory) {
        this.currentInventory = currentInventory;
    }

    public Integer getIncomingUnits() {
        return incomingUnits;
    }

    public void setIncomingUnits(Integer incomingUnits) {
        this.incomingUnits = incomingUnits;
    }

    public Integer getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(Integer safetyStock) {
        this.safetyStock = safetyStock;
    }

    public Integer getRequiredStock() {
        return requiredStock;
    }

    public void setRequiredStock(Integer requiredStock) {
        this.requiredStock = requiredStock;
    }

    public Integer getRecommendedShipment() {
        return recommendedShipment;
    }

    public void setRecommendedShipment(Integer recommendedShipment) {
        this.recommendedShipment = recommendedShipment;
    }

    public RecommendationPriority getPriority() {
        return priority;
    }

    public void setPriority(RecommendationPriority priority) {
        this.priority = priority;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

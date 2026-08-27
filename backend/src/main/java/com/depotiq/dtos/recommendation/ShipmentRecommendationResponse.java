package com.depotiq.dtos.recommendation;

import com.depotiq.models.RecommendationPriority;
import com.depotiq.models.RecommendationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ShipmentRecommendationResponse {
    private Long id;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private Integer deliveryLeadTimeDays;
    private Long productId;
    private String productCode;
    private String productName;
    private String category;
    private Long demandForecastId;
    private LocalDate recommendationDate;
    private Integer horizonDays;
    private BigDecimal predictedDemand;
    private BigDecimal confidenceLower;
    private BigDecimal confidenceUpper;
    private Integer currentInventory;
    private Integer incomingUnits;
    private Integer safetyStock;
    private Integer requiredStock;
    private Integer recommendedShipment;
    private Integer originalRecommendedShipment;
    private String overrideReason;
    private String overriddenBy;
    private OffsetDateTime overriddenAt;
    private RecommendationPriority priority;
    private RecommendationStatus status;
    private String explanation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getDeliveryLeadTimeDays() {
        return deliveryLeadTimeDays;
    }

    public void setDeliveryLeadTimeDays(Integer deliveryLeadTimeDays) {
        this.deliveryLeadTimeDays = deliveryLeadTimeDays;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Integer getOriginalRecommendedShipment() {
        return originalRecommendedShipment;
    }

    public void setOriginalRecommendedShipment(Integer originalRecommendedShipment) {
        this.originalRecommendedShipment = originalRecommendedShipment;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    public OffsetDateTime getOverriddenAt() {
        return overriddenAt;
    }

    public void setOverriddenAt(OffsetDateTime overriddenAt) {
        this.overriddenAt = overriddenAt;
    }

    public RecommendationPriority getPriority() {
        return priority;
    }

    public void setPriority(RecommendationPriority priority) {
        this.priority = priority;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

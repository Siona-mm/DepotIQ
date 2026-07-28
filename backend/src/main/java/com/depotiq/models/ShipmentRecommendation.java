package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "shipment_recommendations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_recommendation_store_product_date_horizon",
        columnNames = {
            "store_id",
            "product_id",
            "recommendation_date",
            "horizon_days"
        }
    )
)
public class ShipmentRecommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_forecast_id")
    private DemandForecast demandForecast;

    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays;

    @Column(name = "predicted_demand", nullable = false, precision = 12, scale = 2)
    private BigDecimal predictedDemand;

    @Column(name = "confidence_lower", precision = 12, scale = 2)
    private BigDecimal confidenceLower;

    @Column(name = "confidence_upper", precision = 12, scale = 2)
    private BigDecimal confidenceUpper;

    @Column(name = "current_inventory", nullable = false)
    private Integer currentInventory = 0;

    @Column(name = "incoming_units", nullable = false)
    private Integer incomingUnits = 0;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 0;

    @Column(name = "required_stock", nullable = false)
    private Integer requiredStock = 0;

    @Column(name = "recommended_shipment", nullable = false)
    private Integer recommendedShipment = 0;

    @Column(name = "original_recommended_shipment")
    private Integer originalRecommendedShipment;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "overridden_by", length = 100)
    private String overriddenBy;

    @Column(name = "overridden_at")
    private OffsetDateTime overriddenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public DemandForecast getDemandForecast() {
        return demandForecast;
    }

    public void setDemandForecast(DemandForecast demandForecast) {
        this.demandForecast = demandForecast;
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

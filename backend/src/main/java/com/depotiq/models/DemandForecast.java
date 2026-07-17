package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "demand_forecasts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_forecast_store_product_date_horizon",
        columnNames = {"store_id", "product_id", "forecast_date", "horizon_days"}
    )
)
public class DemandForecast extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays;

    @Column(name = "predicted_demand", nullable = false, precision = 12, scale = 2)
    private BigDecimal predictedDemand;

    @Column(name = "confidence_lower", precision = 12, scale = 2)
    private BigDecimal confidenceLower;

    @Column(name = "confidence_upper", precision = 12, scale = 2)
    private BigDecimal confidenceUpper;

    @Column(name = "model_name", length = 150)
    private String modelName;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "model_mae", precision = 12, scale = 4)
    private BigDecimal modelMae;

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

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public BigDecimal getModelMae() {
        return modelMae;
    }

    public void setModelMae(BigDecimal modelMae) {
        this.modelMae = modelMae;
    }
}

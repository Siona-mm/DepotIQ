package com.depotiq.dtos.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ForecastResponse {
    private Long id;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private Long productId;
    private String productCode;
    private String productName;
    private String category;
    private LocalDate forecastDate;
    private Integer horizonDays;
    private BigDecimal predictedDemand;
    private BigDecimal confidenceLower;
    private BigDecimal confidenceUpper;
    private String modelName;
    private String modelVersion;
    private BigDecimal modelMae;

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

package com.depotiq.dtos.recommendation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OverrideRecommendationRequest {
    @NotNull
    @Min(0)
    private Integer recommendedShipment;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotBlank
    @Size(max = 100)
    private String overriddenBy;

    public Integer getRecommendedShipment() {
        return recommendedShipment;
    }

    public void setRecommendedShipment(Integer recommendedShipment) {
        this.recommendedShipment = recommendedShipment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }
}

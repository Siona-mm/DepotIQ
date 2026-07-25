package com.depotiq.dtos.recommendation;

import com.depotiq.models.RecommendationStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateRecommendationStatusRequest {
    @NotNull
    private RecommendationStatus status;

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }
}

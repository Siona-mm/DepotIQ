package com.depotiq.dtos.shipment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ApproveAndDispatchShipmentRequest {
    @NotEmpty
    private List<@NotNull Long> recommendationIds;

    @Size(max = 500)
    private String notes;

    public List<Long> getRecommendationIds() {
        return recommendationIds;
    }

    public void setRecommendationIds(List<Long> recommendationIds) {
        this.recommendationIds = recommendationIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

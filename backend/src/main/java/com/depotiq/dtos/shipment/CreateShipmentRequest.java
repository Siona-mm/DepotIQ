package com.depotiq.dtos.shipment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class CreateShipmentRequest {

    @NotEmpty
    private List<@NotNull Long> recommendationIds;

    @NotNull
    @FutureOrPresent
    private LocalDate plannedDispatchDate;

    @NotNull
    @FutureOrPresent
    private LocalDate expectedDeliveryDate;

    @Size(max = 500)
    private String notes;

    public List<Long> getRecommendationIds() {
        return recommendationIds;
    }

    public void setRecommendationIds(List<Long> recommendationIds) {
        this.recommendationIds = recommendationIds;
    }

    public LocalDate getPlannedDispatchDate() {
        return plannedDispatchDate;
    }

    public void setPlannedDispatchDate(LocalDate plannedDispatchDate) {
        this.plannedDispatchDate = plannedDispatchDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

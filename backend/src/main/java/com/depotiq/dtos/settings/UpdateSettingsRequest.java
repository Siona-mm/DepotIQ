package com.depotiq.dtos.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequest(
        @NotNull @Min(3) @Max(30) Integer defaultHorizon,
        @NotNull @Min(1) @Max(30) Integer safetyStockDays,
        @NotNull @Min(0) Integer alertThreshold,
        @NotNull Boolean autoRefresh,
        @NotNull Boolean requireApproval,
        @NotNull Boolean allowOverrides,
        @NotNull Boolean emailAlerts
) {
}

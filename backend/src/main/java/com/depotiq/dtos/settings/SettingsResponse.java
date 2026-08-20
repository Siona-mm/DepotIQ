package com.depotiq.dtos.settings;

public record SettingsResponse(
        Integer defaultHorizon,
        Integer safetyStockDays,
        Integer alertThreshold,
        Boolean autoRefresh,
        Boolean requireApproval,
        Boolean allowOverrides,
        Boolean emailAlerts
) {
}

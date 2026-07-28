package com.depotiq.dtos.ml;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MlRecommendationPayload(
        String storeCode,
        String productCode,
        String storeType,
        String category,
        LocalDate forecastDate,
        Integer horizonDays,
        Integer currentInventory,
        Integer incomingUnits,
        BigDecimal predictedDemand,
        BigDecimal confidenceLower,
        BigDecimal confidenceUpper,
        String modelName,
        String modelVersion,
        BigDecimal modelMae,
        Integer safetyStock,
        Integer requiredStock,
        Integer recommendedShipment,
        String priority,
        String explanation
) {
}

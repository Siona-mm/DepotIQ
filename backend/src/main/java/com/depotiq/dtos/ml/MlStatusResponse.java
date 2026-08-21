package com.depotiq.dtos.ml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MlStatusResponse(
        boolean serviceAvailable,
        String serviceStatus,
        List<MlModelInfoResponse> models,
        long forecastCount,
        long recommendationCount,
        long coveredStores,
        long coveredProducts,
        BigDecimal averageMae,
        LocalDate latestForecastDate,
        LocalDateTime lastSynchronizedAt
) {
}

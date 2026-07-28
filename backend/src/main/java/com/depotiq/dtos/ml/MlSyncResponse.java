package com.depotiq.dtos.ml;

import java.time.LocalDate;

public record MlSyncResponse(
        LocalDate sourceDate,
        int received,
        int forecastsSynced,
        int recommendationsSynced,
        int skippedUnknownStoreOrProduct
) {
}

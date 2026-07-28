package com.depotiq.dtos.ml;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MlRecommendationBatchResponse(
        OffsetDateTime generatedAt,
        LocalDate sourceDate,
        Integer totalAvailable,
        Integer returned,
        List<MlRecommendationPayload> recommendations
) {
}

package com.depotiq.dtos.ml;

import java.math.BigDecimal;

public record MlModelInfoResponse(
        Integer horizonDays,
        String modelName,
        String modelVersion,
        BigDecimal mae,
        BigDecimal rmse,
        boolean artifactAvailable
) {
}

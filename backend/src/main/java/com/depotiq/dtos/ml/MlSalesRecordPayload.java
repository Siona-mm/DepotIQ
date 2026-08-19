package com.depotiq.dtos.ml;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MlSalesRecordPayload(
        String storeCode,
        String productCode,
        LocalDate saleDate,
        Integer unitsSold,
        BigDecimal price,
        BigDecimal discount,
        Boolean promotion,
        String weatherCondition,
        Boolean holidayPromotion,
        String seasonality
) {
}

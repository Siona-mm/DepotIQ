package com.depotiq.dtos.importing;

import java.util.List;

public record HistoricalSalesImportResponse(
        int processedRows,
        int createdRecords,
        int updatedRecords,
        int skippedRows,
        List<String> errors
) {
}

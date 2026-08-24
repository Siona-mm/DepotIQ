package com.depotiq.dtos.importing;

import java.util.List;

public record HistoricalSalesImportResponse(
        int processedRows,
        int createdRecords,
        int updatedRecords,
        int skippedRows,
        int createdStores,
        int createdProducts,
        List<String> errors,
        List<String> importedInventoryKeys,
        boolean planningRefreshRequested
) {
}

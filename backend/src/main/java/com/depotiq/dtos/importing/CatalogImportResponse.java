package com.depotiq.dtos.importing;

import java.util.List;

public record CatalogImportResponse(
        String importType,
        String fileName,
        int processedRows,
        int createdRecords,
        int updatedRecords,
        int skippedRows,
        List<String> errors
) {
}

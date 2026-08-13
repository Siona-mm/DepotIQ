package com.depotiq.dtos.importing;

import java.time.LocalDateTime;

public record ImportAuditLogResponse(
        Long id,
        String fileName,
        String importType,
        Integer processedRows,
        Integer createdRecords,
        Integer updatedRecords,
        Integer skippedRows,
        Integer createdStores,
        Integer createdProducts,
        String errorSummary,
        LocalDateTime createdAt
) {
}

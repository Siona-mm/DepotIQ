package com.depotiq.events;

import java.time.LocalDateTime;

public record OperationalDataImportedEvent(LocalDateTime importedAt) {
}

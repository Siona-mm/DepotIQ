package com.depotiq.events;

import com.depotiq.services.MlIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationalDataRefreshListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationalDataRefreshListener.class);

    private final MlIntegrationService mlIntegrationService;

    public OperationalDataRefreshListener(MlIntegrationService mlIntegrationService) {
        this.mlIntegrationService = mlIntegrationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refreshForecastsAndRecommendations(OperationalDataImportedEvent event) {
        try {
            mlIntegrationService.syncImportedData();
            var result = mlIntegrationService.syncRecommendations();
            LOGGER.info(
                    "Refreshed {} forecasts and {} shipment recommendations after import at {}",
                    result.forecastsSynced(),
                    result.recommendationsSynced(),
                    event.importedAt()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Imported operational data at {} but could not refresh ML recommendations: {}",
                    event.importedAt(),
                    exception.getMessage()
            );
        }
    }
}

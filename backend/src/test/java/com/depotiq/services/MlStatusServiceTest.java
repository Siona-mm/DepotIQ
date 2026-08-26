package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.ml.MlHealthResponse;
import com.depotiq.dtos.ml.MlModelInfoResponse;
import com.depotiq.dtos.ml.MlStatusResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MlStatusServiceTest {

    @Test
    void combinesServiceHealthWithPersistedForecastMetrics() {
        MlServiceClient client = mock(MlServiceClient.class);
        DemandForecastRepository forecasts = mock(DemandForecastRepository.class);
        ShipmentRecommendationRepository recommendations =
                mock(ShipmentRecommendationRepository.class);

        Store store = new Store();
        store.setId(1L);
        Product product = new Product();
        product.setId(2L);

        DemandForecast first = forecast(
                store,
                product,
                LocalDate.of(2026, 8, 23),
                LocalDateTime.of(2026, 8, 23, 10, 0),
                "10.00"
        );
        DemandForecast second = forecast(
                store,
                product,
                LocalDate.of(2026, 8, 24),
                LocalDateTime.of(2026, 8, 24, 11, 30),
                "14.00"
        );
        MlModelInfoResponse model = new MlModelInfoResponse(
                7,
                "hist_gradient_boosting",
                "1.0",
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(16),
                true
        );

        when(client.getHealth()).thenReturn(new MlHealthResponse("ok"));
        when(client.getModels()).thenReturn(List.of(model));
        when(forecasts.findAll()).thenReturn(List.of(first, second));
        when(recommendations.count()).thenReturn(9L);

        MlStatusResponse status = new MlStatusService(
                client,
                forecasts,
                recommendations
        ).getStatus();

        assertThat(status.serviceAvailable()).isTrue();
        assertThat(status.models()).containsExactly(model);
        assertThat(status.forecastCount()).isEqualTo(2);
        assertThat(status.recommendationCount()).isEqualTo(9);
        assertThat(status.coveredStores()).isEqualTo(1);
        assertThat(status.coveredProducts()).isEqualTo(1);
        assertThat(status.averageMae()).isEqualByComparingTo("12.00");
        assertThat(status.latestForecastDate()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(status.lastSynchronizedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 24, 11, 30));
    }

    private DemandForecast forecast(
            Store store,
            Product product,
            LocalDate forecastDate,
            LocalDateTime updatedAt,
            String mae
    ) {
        DemandForecast forecast = new DemandForecast();
        forecast.setStore(store);
        forecast.setProduct(product);
        forecast.setForecastDate(forecastDate);
        forecast.setUpdatedAt(updatedAt);
        forecast.setModelMae(new BigDecimal(mae));
        return forecast;
    }
}

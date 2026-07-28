package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.ml.MlRecommendationBatchResponse;
import com.depotiq.dtos.ml.MlRecommendationPayload;
import com.depotiq.dtos.ml.MlSyncResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.models.Store;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import com.depotiq.repositories.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MlIntegrationServiceTest {

    @Test
    void syncsKnownRowsAndSkipsUnknownRows() {
        MlServiceClient client = mock(MlServiceClient.class);
        StoreRepository stores = mock(StoreRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        DemandForecastRepository forecasts = mock(DemandForecastRepository.class);
        ShipmentRecommendationRepository recommendations =
                mock(ShipmentRecommendationRepository.class);

        Store store = new Store();
        store.setId(1L);
        Product product = new Product();
        product.setId(1L);

        MlRecommendationPayload known = payload("S001", "P0001");
        MlRecommendationPayload unknown = payload("S999", "P9999");
        when(client.getRecommendations()).thenReturn(new MlRecommendationBatchResponse(
                OffsetDateTime.now(),
                LocalDate.of(2024, 2, 4),
                2,
                2,
                List.of(known, unknown)
        ));
        when(stores.findByStoreCode("S001")).thenReturn(Optional.of(store));
        when(products.findByProductCode("P0001")).thenReturn(Optional.of(product));
        when(stores.findByStoreCode("S999")).thenReturn(Optional.empty());
        when(products.findByProductCode("P9999")).thenReturn(Optional.empty());
        when(forecasts.findByStoreIdAndProductIdAndForecastDateAndHorizonDays(
                any(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(forecasts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendations
                .findByStoreIdAndProductIdAndRecommendationDateAndHorizonDays(
                        any(), any(), any(), any()
                )).thenReturn(Optional.empty());

        MlIntegrationService service = new MlIntegrationService(
                client,
                stores,
                products,
                forecasts,
                recommendations
        );

        MlSyncResponse response = service.syncRecommendations();

        assertThat(response.received()).isEqualTo(2);
        assertThat(response.forecastsSynced()).isEqualTo(1);
        assertThat(response.recommendationsSynced()).isEqualTo(1);
        assertThat(response.skippedUnknownStoreOrProduct()).isEqualTo(1);
        verify(forecasts).save(any(DemandForecast.class));
        verify(recommendations).save(any(ShipmentRecommendation.class));
    }

    private MlRecommendationPayload payload(String storeCode, String productCode) {
        return new MlRecommendationPayload(
                storeCode,
                productCode,
                "Small",
                "Groceries",
                LocalDate.of(2024, 2, 4),
                3,
                20,
                5,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(48),
                BigDecimal.valueOf(72),
                "hist_gradient_boosting",
                "1.0",
                BigDecimal.valueOf(11.85),
                9,
                69,
                44,
                "URGENT",
                "Validation-based range."
        );
    }
}

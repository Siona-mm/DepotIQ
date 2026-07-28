package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.recommendation.OverrideRecommendationRequest;
import com.depotiq.dtos.recommendation.ShipmentRecommendationResponse;
import com.depotiq.mappers.ShipmentRecommendationMapper;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import com.depotiq.repositories.StoreRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShipmentRecommendationServiceTest {

    @Test
    void persistsOverrideAuditAndMarksRecommendationEdited() {
        ShipmentRecommendationRepository recommendations =
                mock(ShipmentRecommendationRepository.class);
        StoreRepository stores = mock(StoreRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        DemandForecastRepository forecasts = mock(DemandForecastRepository.class);
        ShipmentRecommendationMapper mapper = mock(ShipmentRecommendationMapper.class);

        ShipmentRecommendation recommendation = new ShipmentRecommendation();
        recommendation.setId(12L);
        recommendation.setRecommendedShipment(80);
        recommendation.setStatus(RecommendationStatus.PENDING);

        OverrideRecommendationRequest request = new OverrideRecommendationRequest();
        request.setRecommendedShipment(120);
        request.setReason("Store confirmed a weekend promotion");
        request.setOverriddenBy("SM");

        ShipmentRecommendationResponse response = new ShipmentRecommendationResponse();
        response.setId(12L);

        when(recommendations.findById(12L)).thenReturn(Optional.of(recommendation));
        when(recommendations.save(recommendation)).thenReturn(recommendation);
        when(mapper.toResponse(recommendation)).thenReturn(response);

        ShipmentRecommendationService service = new ShipmentRecommendationService(
                recommendations,
                stores,
                products,
                forecasts,
                mapper
        );

        ShipmentRecommendationResponse result =
                service.overrideRecommendedShipment(12L, request);

        assertThat(result).isSameAs(response);
        assertThat(recommendation.getOriginalRecommendedShipment()).isEqualTo(80);
        assertThat(recommendation.getRecommendedShipment()).isEqualTo(120);
        assertThat(recommendation.getOverrideReason())
                .isEqualTo("Store confirmed a weekend promotion");
        assertThat(recommendation.getOverriddenBy()).isEqualTo("SM");
        assertThat(recommendation.getOverriddenAt()).isNotNull();
        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.EDITED);
        verify(recommendations).save(recommendation);
    }
}

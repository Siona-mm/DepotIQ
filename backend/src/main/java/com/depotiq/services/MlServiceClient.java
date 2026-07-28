package com.depotiq.services;

import com.depotiq.dtos.ml.MlHealthResponse;
import com.depotiq.dtos.ml.MlRecommendationBatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MlServiceClient {
    private final RestClient restClient;

    public MlServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${depotiq.ml-service.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public MlHealthResponse getHealth() {
        try {
            MlHealthResponse response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(MlHealthResponse.class);

            if (response == null) {
                throw unavailable("ML service returned an empty health response", null);
            }

            return response;
        } catch (RestClientException exception) {
            throw unavailable("ML service is unavailable", exception);
        }
    }

    public MlRecommendationBatchResponse getRecommendations() {
        try {
            MlRecommendationBatchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/recommendations")
                            .queryParam("limit", 1000)
                            .build())
                    .retrieve()
                    .body(MlRecommendationBatchResponse.class);

            if (response == null || response.recommendations() == null) {
                throw unavailable("ML service returned an empty recommendation response", null);
            }

            return response;
        } catch (RestClientException exception) {
            throw unavailable("Could not retrieve ML recommendations", exception);
        }
    }

    private ResponseStatusException unavailable(String message, Exception cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, cause);
    }
}

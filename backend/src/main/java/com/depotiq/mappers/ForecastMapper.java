package com.depotiq.mappers;

import com.depotiq.dtos.forecast.CreateForecastRequest;
import com.depotiq.dtos.forecast.ForecastResponse;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import org.springframework.stereotype.Component;

@Component
public class ForecastMapper {
    public ForecastResponse toResponse(DemandForecast forecast) {
        Store store = forecast.getStore();
        Product product = forecast.getProduct();
        ForecastResponse response = new ForecastResponse();

        response.setId(forecast.getId());
        response.setStoreId(store.getId());
        response.setStoreCode(store.getStoreCode());
        response.setStoreName(store.getName());
        response.setProductId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getName());
        response.setCategory(product.getCategory());
        response.setForecastDate(forecast.getForecastDate());
        response.setHorizonDays(forecast.getHorizonDays());
        response.setPredictedDemand(forecast.getPredictedDemand());
        response.setConfidenceLower(forecast.getConfidenceLower());
        response.setConfidenceUpper(forecast.getConfidenceUpper());
        response.setModelName(forecast.getModelName());
        response.setModelVersion(forecast.getModelVersion());
        response.setModelMae(forecast.getModelMae());

        return response;
    }

    public DemandForecast toEntity(CreateForecastRequest request, Store store, Product product) {
        DemandForecast forecast = new DemandForecast();

        forecast.setStore(store);
        forecast.setProduct(product);
        forecast.setForecastDate(request.getForecastDate());
        forecast.setHorizonDays(request.getHorizonDays());
        forecast.setPredictedDemand(request.getPredictedDemand());
        forecast.setConfidenceLower(request.getConfidenceLower());
        forecast.setConfidenceUpper(request.getConfidenceUpper());
        forecast.setModelName(request.getModelName());
        forecast.setModelVersion(request.getModelVersion());
        forecast.setModelMae(request.getModelMae());

        return forecast;
    }
}

package com.depotiq.services;

import com.depotiq.dtos.forecast.CreateForecastRequest;
import com.depotiq.dtos.forecast.ForecastResponse;
import com.depotiq.mappers.ForecastMapper;
import com.depotiq.models.DemandForecast;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.repositories.DemandForecastRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ForecastService {
    private final DemandForecastRepository demandForecastRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ForecastMapper forecastMapper;

    public ForecastService(
            DemandForecastRepository demandForecastRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ForecastMapper forecastMapper
    ) {
        this.demandForecastRepository = demandForecastRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.forecastMapper = forecastMapper;
    }

    public List<ForecastResponse> getAllForecasts() {
        return demandForecastRepository.findAll()
                .stream()
                .map(forecastMapper::toResponse)
                .toList();
    }

    public ForecastResponse getForecastById(Long id) {
        return forecastMapper.toResponse(findForecastOrThrow(id));
    }

    public List<ForecastResponse> getForecastsByStore(Long storeId) {
        return demandForecastRepository.findByStoreId(storeId)
                .stream()
                .map(forecastMapper::toResponse)
                .toList();
    }

    public ForecastResponse createForecast(CreateForecastRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        DemandForecast forecast = forecastMapper.toEntity(request, store, product);
        return forecastMapper.toResponse(demandForecastRepository.save(forecast));
    }

    public void deleteForecast(Long id) {
        DemandForecast forecast = findForecastOrThrow(id);
        demandForecastRepository.delete(forecast);
    }

    private DemandForecast findForecastOrThrow(Long id) {
        return demandForecastRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Forecast not found"));
    }
}

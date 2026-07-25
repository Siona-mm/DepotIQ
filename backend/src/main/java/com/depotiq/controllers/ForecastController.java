package com.depotiq.controllers;

import com.depotiq.dtos.forecast.CreateForecastRequest;
import com.depotiq.dtos.forecast.ForecastResponse;
import com.depotiq.services.ForecastService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {
    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public List<ForecastResponse> getAllForecasts() {
        return forecastService.getAllForecasts();
    }

    @GetMapping("/{id}")
    public ForecastResponse getForecastById(@PathVariable Long id) {
        return forecastService.getForecastById(id);
    }

    @GetMapping("/stores/{storeId}")
    public List<ForecastResponse> getForecastsByStore(@PathVariable Long storeId) {
        return forecastService.getForecastsByStore(storeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForecastResponse createForecast(@Valid @RequestBody CreateForecastRequest request) {
        return forecastService.createForecast(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForecast(@PathVariable Long id) {
        forecastService.deleteForecast(id);
    }
}

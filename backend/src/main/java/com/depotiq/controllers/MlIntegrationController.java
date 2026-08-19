package com.depotiq.controllers;

import com.depotiq.dtos.ml.MlHealthResponse;
import com.depotiq.dtos.ml.MlDataSyncResponse;
import com.depotiq.dtos.ml.MlSyncResponse;
import com.depotiq.services.MlIntegrationService;
import com.depotiq.services.MlServiceClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ml")
public class MlIntegrationController {
    private final MlServiceClient mlServiceClient;
    private final MlIntegrationService mlIntegrationService;

    public MlIntegrationController(
            MlServiceClient mlServiceClient,
            MlIntegrationService mlIntegrationService
    ) {
        this.mlServiceClient = mlServiceClient;
        this.mlIntegrationService = mlIntegrationService;
    }

    @GetMapping("/health")
    public MlHealthResponse getHealth() {
        return mlServiceClient.getHealth();
    }

    @PostMapping("/sync")
    public MlSyncResponse syncRecommendations() {
        return mlIntegrationService.syncRecommendations();
    }

    @PostMapping("/data-sync")
    public MlDataSyncResponse syncImportedData() {
        return mlIntegrationService.syncImportedData();
    }
}

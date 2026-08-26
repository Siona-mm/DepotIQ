package com.depotiq.controllers;

import com.depotiq.dtos.ml.MlHealthResponse;
import com.depotiq.dtos.ml.MlDataSyncResponse;
import com.depotiq.dtos.ml.MlSyncResponse;
import com.depotiq.dtos.ml.MlStatusResponse;
import com.depotiq.services.MlIntegrationService;
import com.depotiq.services.MlServiceClient;
import com.depotiq.services.MlStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ml")
public class MlIntegrationController {
    private final MlServiceClient mlServiceClient;
    private final MlIntegrationService mlIntegrationService;
    private final MlStatusService mlStatusService;

    public MlIntegrationController(
            MlServiceClient mlServiceClient,
            MlIntegrationService mlIntegrationService,
            MlStatusService mlStatusService
    ) {
        this.mlServiceClient = mlServiceClient;
        this.mlIntegrationService = mlIntegrationService;
        this.mlStatusService = mlStatusService;
    }

    @GetMapping("/health")
    public MlHealthResponse getHealth() {
        return mlServiceClient.getHealth();
    }

    @GetMapping("/status")
    public MlStatusResponse getStatus() {
        return mlStatusService.getStatus();
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

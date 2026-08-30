package com.depotiq.controllers;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.dtos.importing.CatalogImportResponse;
import com.depotiq.dtos.importing.ImportAuditLogResponse;
import com.depotiq.services.HistoricalSalesImportService;
import com.depotiq.services.CatalogCsvImportService;
import com.depotiq.services.DepotCsvImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class HistoricalDataImportController {

    private final HistoricalSalesImportService historicalSalesImportService;
    private final CatalogCsvImportService catalogCsvImportService;
    private final DepotCsvImportService depotCsvImportService;

    public HistoricalDataImportController(
            HistoricalSalesImportService historicalSalesImportService,
            CatalogCsvImportService catalogCsvImportService,
            DepotCsvImportService depotCsvImportService
    ) {
        this.historicalSalesImportService = historicalSalesImportService;
        this.catalogCsvImportService = catalogCsvImportService;
        this.depotCsvImportService = depotCsvImportService;
    }

    @PostMapping(value = "/sales-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HistoricalSalesImportResponse importSalesRecords(@RequestParam("file") MultipartFile file) {
        return historicalSalesImportService.importCsv(file);
    }

    @PostMapping(value = "/stores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogImportResponse importStores(@RequestParam("file") MultipartFile file) {
        return catalogCsvImportService.importStores(file);
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogImportResponse importProducts(@RequestParam("file") MultipartFile file) {
        return catalogCsvImportService.importProducts(file);
    }

    @PostMapping(value = "/depot-products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogImportResponse addDepotProducts(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "receiptId", required = false) String receiptId) {
        return depotCsvImportService.addProducts(file, receiptId);
    }

    @PostMapping(value = "/depot-refills", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogImportResponse refillDepot(@RequestParam("file") MultipartFile file) {
        return depotCsvImportService.refill(file);
    }

    @GetMapping
    public List<ImportAuditLogResponse> getImportHistory() {
        return historicalSalesImportService.getImportHistory();
    }
}

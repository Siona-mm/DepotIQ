package com.depotiq.services;

import com.depotiq.dtos.importing.CatalogImportResponse;
import com.depotiq.models.ImportAuditLog;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.models.StoreType;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.StoreRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CatalogCsvImportService {
    private static final int MAX_REPORTED_ERRORS = 100;
    private static final Set<String> STORE_COLUMNS = Set.of(
            "External Store ID", "Name", "Store Type", "Region", "Has Warehouse",
            "Storage Capacity", "Delivery Lead Time Days", "Preferred Horizon Days"
    );
    private static final Set<String> PRODUCT_COLUMNS = Set.of(
            "External SKU", "Name", "Category", "Perishable"
    );

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ImportAuditLogRepository importAuditLogRepository;
    private final BusinessCodeGenerator businessCodeGenerator;

    public CatalogCsvImportService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ImportAuditLogRepository importAuditLogRepository,
            BusinessCodeGenerator businessCodeGenerator
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.importAuditLogRepository = importAuditLogRepository;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CatalogImportResponse importStores(MultipartFile file) {
        return importCsv(file, "STORE_CATALOG", STORE_COLUMNS, this::upsertStore);
    }

    public CatalogImportResponse importProducts(MultipartFile file) {
        return importCsv(file, "PRODUCT_CATALOG", PRODUCT_COLUMNS, this::upsertProduct);
    }

    private CatalogImportResponse importCsv(
            MultipartFile file,
            String importType,
            Set<String> requiredColumns,
            RowImporter rowImporter
    ) {
        validateFile(file);
        String fileName = safeFileName(file.getOriginalFilename());
        int processed = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            removeByteOrderMark(reader);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = format.parse(reader)) {
                validateHeaders(parser.getHeaderMap().keySet(), requiredColumns);
                for (CSVRecord row : parser) {
                    processed++;
                    try {
                        if (rowImporter.importRow(row)) {
                            created++;
                        } else {
                            updated++;
                        }
                    } catch (IllegalArgumentException exception) {
                        skipped++;
                        if (errors.size() < MAX_REPORTED_ERRORS) {
                            errors.add("Line " + (row.getRecordNumber() + 1) + ": " + exception.getMessage());
                        }
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The CSV file could not be read.", exception);
        }

        CatalogImportResponse response = new CatalogImportResponse(
                importType,
                fileName,
                processed,
                created,
                updated,
                skipped,
                List.copyOf(errors)
        );
        saveAuditLog(response);
        return response;
    }

    private boolean upsertStore(CSVRecord row) {
        String externalStoreId = required(row, "External Store ID");
        Store store = storeRepository.findByExternalStoreIdIgnoreCase(externalStoreId).orElse(null);
        boolean created = store == null;
        if (created) {
            store = new Store();
            store.setStoreCode(businessCodeGenerator.nextStoreCode());
            store.setExternalStoreId(externalStoreId);
        }

        store.setName(required(row, "Name"));
        store.setStoreType(parseStoreType(required(row, "Store Type")));
        store.setRegion(required(row, "Region"));
        store.setHasWarehouse(parseBoolean(required(row, "Has Warehouse"), "Has Warehouse"));
        store.setStorageCapacity(parsePositiveInteger(required(row, "Storage Capacity"), "Storage Capacity"));
        store.setDeliveryLeadTimeDays(parsePositiveInteger(
                required(row, "Delivery Lead Time Days"),
                "Delivery Lead Time Days"
        ));
        store.setPreferredHorizonDays(parsePositiveInteger(
                required(row, "Preferred Horizon Days"),
                "Preferred Horizon Days"
        ));
        storeRepository.save(store);
        return created;
    }

    private boolean upsertProduct(CSVRecord row) {
        String externalSku = required(row, "External SKU");
        Product product = productRepository.findByExternalSkuIgnoreCase(externalSku).orElse(null);
        boolean created = product == null;
        if (created) {
            product = new Product();
            product.setProductCode(businessCodeGenerator.nextProductCode());
            product.setExternalSku(externalSku);
        }

        product.setName(required(row, "Name"));
        product.setCategory(required(row, "Category"));
        product.setBrand(optional(row, "Brand"));
        product.setSupplierCode(optional(row, "Supplier Code"));
        product.setUnitCost(parseOptionalDecimal(row, "Unit Cost"));
        product.setPrice(parseOptionalDecimal(row, "Price"));
        product.setWeightKg(parseOptionalDecimal(row, "Weight Kg"));
        product.setShelfLifeDays(parseOptionalNonNegativeInteger(row, "Shelf Life Days"));
        product.setPerishable(parseBoolean(required(row, "Perishable"), "Perishable"));
        productRepository.save(product);
        return created;
    }

    private void saveAuditLog(CatalogImportResponse response) {
        ImportAuditLog auditLog = new ImportAuditLog();
        auditLog.setFileName(response.fileName());
        auditLog.setImportType(response.importType());
        auditLog.setProcessedRows(response.processedRows());
        auditLog.setCreatedRecords(response.createdRecords());
        auditLog.setUpdatedRecords(response.updatedRecords());
        auditLog.setSkippedRows(response.skippedRows());
        auditLog.setCreatedStores("STORE_CATALOG".equals(response.importType()) ? response.createdRecords() : 0);
        auditLog.setCreatedProducts("PRODUCT_CATALOG".equals(response.importType()) ? response.createdRecords() : 0);
        auditLog.setErrorSummary(response.errors().isEmpty() ? null : String.join("\n", response.errors()));
        importAuditLogRepository.save(auditLog);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty CSV file is required.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported.");
        }
    }

    private void validateHeaders(Set<String> actualColumns, Set<String> requiredColumns) {
        List<String> missing = requiredColumns.stream()
                .filter(required -> actualColumns.stream().noneMatch(actual -> actual.equalsIgnoreCase(required)))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missing));
        }
    }

    private StoreType parseStoreType(String value) {
        try {
            return StoreType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Store Type must be Small, Medium, Large, or Warehouse Store."
            );
        }
    }

    private boolean parseBoolean(String value, String field) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new IllegalArgumentException(field + " must be true or false.");
        };
    }

    private int parsePositiveInteger(String value, String field) {
        int parsed = parseInteger(value, field);
        if (parsed < 1) {
            throw new IllegalArgumentException(field + " must be at least 1.");
        }
        return parsed;
    }

    private Integer parseOptionalNonNegativeInteger(CSVRecord row, String field) {
        String value = optional(row, field);
        if (value == null) {
            return null;
        }
        int parsed = parseInteger(value, field);
        if (parsed < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
        return parsed;
    }

    private int parseInteger(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    private BigDecimal parseOptionalDecimal(CSVRecord row, String field) {
        String value = optional(row, field);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException(field + " cannot be negative.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a number.");
        }
    }

    private String required(CSVRecord row, String field) {
        String value = optional(row, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private String optional(CSVRecord row, String field) {
        if (!row.isMapped(field)) {
            return null;
        }
        String value = row.get(field);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void removeByteOrderMark(BufferedReader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != '\ufeff') {
            reader.reset();
        }
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "catalog.csv";
        }
        return fileName.replace('\\', '/').substring(fileName.replace('\\', '/').lastIndexOf('/') + 1);
    }

    @FunctionalInterface
    private interface RowImporter {
        boolean importRow(CSVRecord row);
    }
}

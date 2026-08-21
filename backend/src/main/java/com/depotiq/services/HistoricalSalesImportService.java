package com.depotiq.services;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.dtos.importing.ImportAuditLogResponse;
import com.depotiq.models.Product;
import com.depotiq.models.ImportAuditLog;
import com.depotiq.models.SalesRecord;
import com.depotiq.models.Store;
import com.depotiq.models.StoreInventory;
import com.depotiq.models.StoreType;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.SalesRecordRepository;
import com.depotiq.repositories.StoreRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class HistoricalSalesImportService {

    private static final String[] REQUIRED_COLUMNS = {
            "Date", "Store ID", "Product ID", "Category", "Region", "Inventory Level",
            "Units Sold", "Units Ordered", "Price", "Discount", "Weather Condition",
            "Holiday/Promotion", "Seasonality"
    };
    private static final String DEFAULT_SOURCE_SYSTEM = "CSV_UPLOAD";

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ImportAuditLogRepository importAuditLogRepository;

    public HistoricalSalesImportService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            SalesRecordRepository salesRecordRepository,
            StoreInventoryRepository storeInventoryRepository,
            ImportAuditLogRepository importAuditLogRepository
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.salesRecordRepository = salesRecordRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.importAuditLogRepository = importAuditLogRepository;
    }

    public HistoricalSalesImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty CSV file is required.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            removeByteOrderMark(reader);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

            int processed = 0;
            int created = 0;
            int updated = 0;
            int createdStores = 0;
            int createdProducts = 0;
            List<String> errors = new ArrayList<>();
            try (CSVParser parser = format.parse(reader)) {
                validateHeader(parser.getHeaderMap().keySet());

                for (CSVRecord csvRecord : parser) {
                    processed++;
                    try {
                        ImportRowResult result = importRow(csvRecord);
                        if (result.salesRecordCreated()) {
                            created++;
                        } else {
                            updated++;
                        }
                        createdStores += result.storeCreated() ? 1 : 0;
                        createdProducts += result.productCreated() ? 1 : 0;
                    } catch (IllegalArgumentException exception) {
                        errors.add("Line " + (csvRecord.getRecordNumber() + 1) + ": " + exception.getMessage());
                    }
                }
            }

            HistoricalSalesImportResponse response = new HistoricalSalesImportResponse(
                    processed,
                    created,
                    updated,
                    errors.size(),
                    createdStores,
                    createdProducts,
                    List.copyOf(errors)
            );
            saveAuditLog(file.getOriginalFilename(), response);
            return response;
        } catch (IOException exception) {
            throw new IllegalArgumentException("The CSV file could not be read.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ImportAuditLogResponse> getImportHistory() {
        return importAuditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(auditLog -> new ImportAuditLogResponse(
                        auditLog.getId(),
                        auditLog.getFileName(),
                        auditLog.getImportType(),
                        auditLog.getProcessedRows(),
                        auditLog.getCreatedRecords(),
                        auditLog.getUpdatedRecords(),
                        auditLog.getSkippedRows(),
                        auditLog.getCreatedStores(),
                        auditLog.getCreatedProducts(),
                        auditLog.getErrorSummary(),
                        auditLog.getCreatedAt()
                ))
                .toList();
    }

    private void saveAuditLog(String fileName, HistoricalSalesImportResponse response) {
        ImportAuditLog auditLog = new ImportAuditLog();
        auditLog.setFileName(fileName == null || fileName.isBlank() ? "unnamed.csv" : fileName);
        auditLog.setImportType("HISTORICAL_SALES");
        auditLog.setProcessedRows(response.processedRows());
        auditLog.setCreatedRecords(response.createdRecords());
        auditLog.setUpdatedRecords(response.updatedRecords());
        auditLog.setSkippedRows(response.skippedRows());
        auditLog.setCreatedStores(response.createdStores());
        auditLog.setCreatedProducts(response.createdProducts());
        auditLog.setErrorSummary(response.errors().isEmpty() ? null : String.join("\n", response.errors()));
        importAuditLogRepository.save(auditLog);
    }

    private ImportRowResult importRow(CSVRecord row) {
        LocalDate saleDate = parseDate(value(row, "Date"));
        String storeCode = requiredValue(value(row, "Store ID"), "Store ID");
        String productCode = requiredValue(value(row, "Product ID"), "Product ID");
        CatalogUpsertResult<Store> storeResult = upsertStore(storeCode, value(row, "Region"));
        BigDecimal price = parseDecimal(value(row, "Price"), "Price");
        CatalogUpsertResult<Product> productResult = upsertProduct(
                productCode,
                value(row, "Category"),
                price
        );
        Store store = storeResult.entity();
        Product product = productResult.entity();
        String sourceSystem = optionalValue(row, "Source System");
        if (sourceSystem == null) {
            sourceSystem = DEFAULT_SOURCE_SYSTEM;
        }
        String externalRecordId = optionalValue(row, "External Record ID");
        if (externalRecordId == null) {
            externalRecordId = storeCode + "-" + productCode + "-" + saleDate;
        }

        Optional<SalesRecord> existing = salesRecordRepository
                .findBySourceSystemAndExternalRecordId(sourceSystem, externalRecordId)
                .or(() -> salesRecordRepository.findByStoreIdAndProductIdAndSaleDate(
                        store.getId(),
                        product.getId(),
                        saleDate
                ));
        SalesRecord record = existing.orElseGet(SalesRecord::new);
        record.setStore(store);
        record.setProduct(product);
        record.setSaleDate(saleDate);
        record.setUnitsSold(parseNonNegativeInteger(value(row, "Units Sold"), "Units Sold"));
        record.setPrice(price);
        record.setDiscount(parseDecimal(value(row, "Discount"), "Discount"));
        record.setWeatherCondition(blankToNull(value(row, "Weather Condition")));
        boolean holidayPromotion = parseBoolean(
                value(row, "Holiday/Promotion"),
                "Holiday/Promotion"
        );
        record.setPromotion(holidayPromotion);
        record.setHolidayPromotion(holidayPromotion);
        record.setSeasonality(blankToNull(value(row, "Seasonality")));
        record.setSourceSystem(sourceSystem);
        record.setExternalRecordId(externalRecordId);
        record.setImportedAt(LocalDateTime.now());
        salesRecordRepository.save(record);

        updateStoreInventory(
                store,
                product,
                parseNonNegativeInteger(value(row, "Inventory Level"), "Inventory Level"),
                parseNonNegativeInteger(value(row, "Units Ordered"), "Units Ordered")
        );

        return new ImportRowResult(existing.isEmpty(), storeResult.created(), productResult.created());
    }

    private void updateStoreInventory(
            Store store,
            Product product,
            int inventoryLevel,
            int incomingUnits
    ) {
        StoreInventory inventory = storeInventoryRepository
                .findByStoreIdAndProductId(store.getId(), product.getId())
                .orElseGet(StoreInventory::new);
        inventory.setStore(store);
        inventory.setProduct(product);
        inventory.setInventoryLevel(inventoryLevel);
        inventory.setIncomingUnits(incomingUnits);
        inventory.setLastUpdated(LocalDateTime.now());
        storeInventoryRepository.save(inventory);
    }

    private CatalogUpsertResult<Store> upsertStore(String storeCode, String regionValue) {
        String region = blankToNull(regionValue);
        Optional<Store> existing = storeRepository.findByStoreCode(storeCode);
        if (existing.isPresent()) {
            Store store = existing.get();
            if (region != null && !region.equals(store.getRegion())) {
                store.setRegion(region);
                storeRepository.save(store);
            }
            return new CatalogUpsertResult<>(store, false);
        }

        Store store = new Store();
        store.setStoreCode(storeCode);
        store.setName("Imported Store " + storeCode);
        store.setStoreType(StoreType.MEDIUM);
        store.setRegion(region);
        store.setHasWarehouse(false);
        store.setStorageCapacity(0);
        store.setDeliveryLeadTimeDays(0);
        store.setPreferredHorizonDays(7);
        return new CatalogUpsertResult<>(storeRepository.save(store), true);
    }

    private CatalogUpsertResult<Product> upsertProduct(
            String productCode,
            String categoryValue,
            BigDecimal price
    ) {
        String category = requiredValue(categoryValue, "Category");
        Optional<Product> existing = productRepository.findByProductCode(productCode);
        if (existing.isPresent()) {
            Product product = existing.get();
            if (!category.equals(product.getCategory()) || hasDifferentValue(product.getPrice(), price)) {
                product.setCategory(category);
                product.setPrice(price);
                productRepository.save(product);
            }
            return new CatalogUpsertResult<>(product, false);
        }

        Product product = new Product();
        product.setProductCode(productCode);
        product.setName("Imported Product " + productCode);
        product.setCategory(category);
        product.setPrice(price);
        product.setPerishable(false);
        return new CatalogUpsertResult<>(productRepository.save(product), true);
    }

    private boolean hasDifferentValue(BigDecimal current, BigDecimal updated) {
        return current == null || current.compareTo(updated) != 0;
    }

    private void validateHeader(Set<String> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("The CSV file is empty.");
        }

        for (String requiredColumn : REQUIRED_COLUMNS) {
            if (!columns.contains(requiredColumn)) {
                throw new IllegalArgumentException("Missing required column: " + requiredColumn);
            }
        }
    }

    private void removeByteOrderMark(BufferedReader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != '\uFEFF') {
            reader.reset();
        }
    }

    private String value(CSVRecord row, String column) {
        try {
            return row.get(column);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Missing required column: " + column);
        }
    }

    private String optionalValue(CSVRecord row, String column) {
        if (!row.isMapped(column)) {
            return null;
        }
        return blankToNull(row.get(column));
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(requiredValue(value, "Date"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid Date value.");
        }
    }

    private Integer parseNonNegativeInteger(String value, String field) {
        try {
            int parsed = Integer.parseInt(requiredValue(value, field));
            if (parsed < 0) {
                throw new IllegalArgumentException(field + " must not be negative.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid " + field + " value.");
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        try {
            return new BigDecimal(requiredValue(value, field));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid " + field + " value.");
        }
    }

    private boolean parseBoolean(String value, String field) {
        String normalized = requiredValue(value, field).trim();
        if ("1".equals(normalized) || "true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("invalid " + field + " value.");
    }

    private String requiredValue(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record CatalogUpsertResult<T>(T entity, boolean created) {
    }

    private record ImportRowResult(
            boolean salesRecordCreated,
            boolean storeCreated,
            boolean productCreated
    ) {
    }
}

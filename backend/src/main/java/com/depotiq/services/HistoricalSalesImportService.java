package com.depotiq.services;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.dtos.importing.ImportAuditLogResponse;
import com.depotiq.events.OperationalDataImportedEvent;
import com.depotiq.models.Product;
import com.depotiq.models.ImportAuditLog;
import com.depotiq.models.Store;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private static final int BATCH_SIZE = 1_000;

    private static final String SALES_UPSERT_SQL = """
            INSERT INTO sales_records (
                store_id, product_id, sale_date, units_sold, price, discount,
                promotion, weather_condition, temperature, holiday_promotion,
                seasonality, source_system, external_record_id, imported_at,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (store_id, product_id, sale_date) DO UPDATE SET
                units_sold = EXCLUDED.units_sold,
                price = EXCLUDED.price,
                discount = EXCLUDED.discount,
                promotion = EXCLUDED.promotion,
                weather_condition = EXCLUDED.weather_condition,
                temperature = EXCLUDED.temperature,
                holiday_promotion = EXCLUDED.holiday_promotion,
                seasonality = EXCLUDED.seasonality,
                source_system = EXCLUDED.source_system,
                external_record_id = EXCLUDED.external_record_id,
                imported_at = EXCLUDED.imported_at,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String INVENTORY_UPSERT_SQL = """
            INSERT INTO store_inventory (
                store_id, product_id, inventory_level, incoming_units,
                last_updated, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (store_id, product_id) DO UPDATE SET
                inventory_level = EXCLUDED.inventory_level,
                incoming_units = EXCLUDED.incoming_units,
                last_updated = EXCLUDED.last_updated,
                updated_at = CURRENT_TIMESTAMP
            """;

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ImportAuditLogRepository importAuditLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public HistoricalSalesImportService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ImportAuditLogRepository importAuditLogRepository,
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher eventPublisher
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.importAuditLogRepository = importAuditLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
    }

    public HistoricalSalesImportResponse importCsv(MultipartFile file) {
        Map<String, Store> storesByCode = loadStores();
        Map<String, Product> productsByCode = loadProducts();
        LocalDateTime importedAt = LocalDateTime.now();
        List<ImportRowResult> rows = CsvImportSupport.read(file, List.of(REQUIRED_COLUMNS),
                row -> importRow(row, storesByCode, productsByCode, importedAt));
        Set<SalesKey> existingSalesKeys = loadExistingSalesKeys();
        Set<String> importedInventoryKeys = new LinkedHashSet<>();
        Map<InventoryKey, ImportedRow> latestInventoryRows = new HashMap<>();
        List<ImportedRow> batch = new ArrayList<>(BATCH_SIZE);
        int created = 0;
        for (ImportRowResult result : rows) {
            if (existingSalesKeys.add(result.row().salesKey())) created++;
            importedInventoryKeys.add(result.inventoryKey());
            batch.add(result.row());
            latestInventoryRows.merge(result.row().inventoryKey(), result.row(),
                    (current, candidate) -> !candidate.saleDate().isBefore(current.saleDate()) ? candidate : current);
            if (batch.size() == BATCH_SIZE) {
                writeSalesBatch(batch);
                batch.clear();
            }
        }
        writeSalesBatch(batch);
        writeInventoryBatch(new ArrayList<>(latestInventoryRows.values()));
        HistoricalSalesImportResponse response = new HistoricalSalesImportResponse(
                rows.size(), created, rows.size() - created, 0, 0, 0, List.of(),
                List.copyOf(importedInventoryKeys), true);
        saveAuditLog(file.getOriginalFilename(), response);
        eventPublisher.publishEvent(new OperationalDataImportedEvent(importedAt));
        return response;
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
        auditLog.setFileName(safeFileName(fileName));
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

    private ImportRowResult importRow(
            CSVRecord row,
            Map<String, Store> storesByCode,
            Map<String, Product> productsByCode,
            LocalDateTime importedAt
    ) {
        LocalDate saleDate = parseDate(value(row, "Date"));
        String storeCode = BusinessCodes.normalizeStoreCode(requiredValue(value(row, "Store ID"), "Store ID"));
        String productCode = requiredValue(value(row, "Product ID"), "Product ID");
        Store store = storesByCode.get(storeCode);
        Product product = productsByCode.get(productCode);
        if (store == null) {
            throw new IllegalArgumentException("Unknown Store ID: " + storeCode
                    + ". Import the complete store catalog first, then use its DepotIQ store code.");
        }
        if (product == null) {
            throw new IllegalArgumentException("Unknown Product ID: " + productCode
                    + ". Import the complete product catalog first, then use its DepotIQ product code.");
        }
        CsvImportSupport.text(row, "Region", 100);
        CsvImportSupport.text(row, "Category", 100);
        BigDecimal price = CsvImportSupport.decimal(row, "Price", 12, 2);
        BigDecimal discount = CsvImportSupport.decimal(row, "Discount", 5, 2);
        if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }
        String sourceSystem = optionalValue(row, "Source System");
        if (sourceSystem == null) {
            sourceSystem = DEFAULT_SOURCE_SYSTEM;
        }
        String externalRecordId = optionalValue(row, "External Record ID");
        if (externalRecordId == null) {
            externalRecordId = storeCode + "-" + productCode + "-" + saleDate;
        }

        boolean holidayPromotion = parseBoolean(
                value(row, "Holiday/Promotion"),
                "Holiday/Promotion"
        );
        ImportedRow importedRow = new ImportedRow(
                store.getId(),
                product.getId(),
                saleDate,
                parseNonNegativeInteger(value(row, "Units Sold"), "Units Sold"),
                price,
                discount,
                optionalBoolean(row, "Promotion", holidayPromotion),
                CsvImportSupport.text(row, "Weather Condition", 100),
                optionalDecimal(row, "Temperature"),
                holidayPromotion,
                CsvImportSupport.text(row, "Seasonality", 100),
                sourceSystem,
                externalRecordId,
                importedAt,
                parseNonNegativeInteger(value(row, "Inventory Level"), "Inventory Level"),
                parseNonNegativeInteger(value(row, "Units Ordered"), "Units Ordered")
        );

        return new ImportRowResult(
                importedRow,
                false,
                false,
                storeCode + "::" + productCode
        );
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed.csv";
        }
        String normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private void writeSalesBatch(List<ImportedRow> rows) {
        if (rows.isEmpty()) {
            return;
        }

        List<Object[]> salesParameters = rows.stream().map(ImportedRow::salesParameters).toList();
        jdbcTemplate.batchUpdate(SALES_UPSERT_SQL, salesParameters);
    }

    private void writeInventoryBatch(List<ImportedRow> rows) {
        if (rows.isEmpty()) {
            return;
        }

        List<Object[]> inventoryParameters = rows.stream().map(ImportedRow::inventoryParameters).toList();
        jdbcTemplate.batchUpdate(INVENTORY_UPSERT_SQL, inventoryParameters);
    }

    private Map<String, Store> loadStores() {
        Map<String, Store> stores = new HashMap<>();
        storeRepository.findAll().forEach(store -> stores.put(store.getStoreCode(), store));
        return stores;
    }

    private Map<String, Product> loadProducts() {
        Map<String, Product> products = new HashMap<>();
        productRepository.findAll().forEach(product -> products.put(product.getProductCode(), product));
        return products;
    }

    private Set<SalesKey> loadExistingSalesKeys() {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT store_id, product_id, sale_date FROM sales_records",
                (resultSet, rowNumber) -> new SalesKey(
                        resultSet.getLong("store_id"),
                        resultSet.getLong("product_id"),
                        resultSet.getObject("sale_date", LocalDate.class)
                )
        ));
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

    private BigDecimal optionalDecimal(CSVRecord row, String column) {
        String value = optionalValue(row, column);
        return value == null ? null : parseDecimal(value, column);
    }

    private boolean optionalBoolean(CSVRecord row, String column, boolean defaultValue) {
        String value = optionalValue(row, column);
        return value == null ? defaultValue : parseBoolean(value, column);
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

    private record SalesKey(Long storeId, Long productId, LocalDate saleDate) {
    }

    private record InventoryKey(Long storeId, Long productId) {
    }

    private record ImportedRow(
            Long storeId,
            Long productId,
            LocalDate saleDate,
            Integer unitsSold,
            BigDecimal price,
            BigDecimal discount,
            Boolean promotion,
            String weatherCondition,
            BigDecimal temperature,
            Boolean holidayPromotion,
            String seasonality,
            String sourceSystem,
            String externalRecordId,
            LocalDateTime importedAt,
            Integer inventoryLevel,
            Integer incomingUnits
    ) {
        SalesKey salesKey() {
            return new SalesKey(storeId, productId, saleDate);
        }

        InventoryKey inventoryKey() {
            return new InventoryKey(storeId, productId);
        }

        Object[] salesParameters() {
            return new Object[] {
                    storeId, productId, saleDate, unitsSold, price, discount, promotion,
                    weatherCondition, temperature, holidayPromotion, seasonality,
                    sourceSystem, externalRecordId, importedAt
            };
        }

        Object[] inventoryParameters() {
            return new Object[] {
                    storeId, productId, inventoryLevel, incomingUnits, importedAt
            };
        }
    }

    private record ImportRowResult(
            ImportedRow row,
            boolean storeCreated,
            boolean productCreated,
            String inventoryKey
    ) {
    }
}

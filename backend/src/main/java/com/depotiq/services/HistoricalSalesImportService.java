package com.depotiq.services;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.models.Product;
import com.depotiq.models.SalesRecord;
import com.depotiq.models.Store;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.SalesRecordRepository;
import com.depotiq.repositories.StoreRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class HistoricalSalesImportService {

    private static final String[] REQUIRED_COLUMNS = {
            "Date", "Store ID", "Product ID", "Units Sold", "Price", "Discount",
            "Weather Condition", "Holiday/Promotion", "Seasonality"
    };

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SalesRecordRepository salesRecordRepository;

    public HistoricalSalesImportService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            SalesRecordRepository salesRecordRepository
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.salesRecordRepository = salesRecordRepository;
    }

    public HistoricalSalesImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty CSV file is required.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String header = reader.readLine();
            validateHeader(header);

            int processed = 0;
            int created = 0;
            int updated = 0;
            List<String> errors = new ArrayList<>();
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                processed++;
                try {
                    boolean wasCreated = importRow(line);
                    if (wasCreated) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (IllegalArgumentException exception) {
                    errors.add("Line " + lineNumber + ": " + exception.getMessage());
                }
            }

            return new HistoricalSalesImportResponse(processed, created, updated, errors.size(), List.copyOf(errors));
        } catch (IOException exception) {
            throw new IllegalArgumentException("The CSV file could not be read.", exception);
        }
    }

    private boolean importRow(String line) {
        String[] values = line.split(",", -1);
        if (values.length < 15) {
            throw new IllegalArgumentException("expected 15 columns.");
        }

        LocalDate saleDate = parseDate(values[0]);
        String storeCode = requiredValue(values[1], "Store ID");
        String productCode = requiredValue(values[2], "Product ID");
        Store store = storeRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown store '" + storeCode + "'."));
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown product '" + productCode + "'."));

        Optional<SalesRecord> existing = salesRecordRepository
                .findByStoreIdAndProductIdAndSaleDate(store.getId(), product.getId(), saleDate);
        SalesRecord record = existing.orElseGet(SalesRecord::new);
        record.setStore(store);
        record.setProduct(product);
        record.setSaleDate(saleDate);
        record.setUnitsSold(parseNonNegativeInteger(values[6], "Units Sold"));
        record.setPrice(parseDecimal(values[9], "Price"));
        record.setDiscount(parseDecimal(values[10], "Discount"));
        record.setWeatherCondition(blankToNull(values[11]));
        boolean holidayPromotion = parseBoolean(values[12], "Holiday/Promotion");
        record.setPromotion(holidayPromotion);
        record.setHolidayPromotion(holidayPromotion);
        record.setSeasonality(blankToNull(values[14]));
        salesRecordRepository.save(record);

        return existing.isEmpty();
    }

    private void validateHeader(String header) {
        if (header == null) {
            throw new IllegalArgumentException("The CSV file is empty.");
        }

        String[] columns = header.replace("\uFEFF", "").split(",", -1);
        for (String requiredColumn : REQUIRED_COLUMNS) {
            boolean found = false;
            for (String column : columns) {
                if (requiredColumn.equals(column.trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Missing required column: " + requiredColumn);
            }
        }
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
}

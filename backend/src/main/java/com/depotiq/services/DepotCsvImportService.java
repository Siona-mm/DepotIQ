package com.depotiq.services;

import com.depotiq.dtos.importing.CatalogImportResponse;
import com.depotiq.models.Product;
import com.depotiq.repositories.ProductRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DepotCsvImportService {
    private final ProductRepository productRepository;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final CatalogCsvImportService catalogCsvImportService;

    public DepotCsvImportService(ProductRepository productRepository, BusinessCodeGenerator businessCodeGenerator,
            JdbcTemplate jdbcTemplate, CatalogCsvImportService catalogCsvImportService) {
        this.productRepository = productRepository;
        this.businessCodeGenerator = businessCodeGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.catalogCsvImportService = catalogCsvImportService;
    }

    public CatalogImportResponse addProducts(MultipartFile file) {
        return addProducts(file, null);
    }

    public CatalogImportResponse addProducts(MultipartFile file, String deliveryReference) {
        var identifiers = new HashSet<String>();
        var rows = CsvImportSupport.read(file, CatalogCsvImportService.PRODUCT_COLUMNS, row -> {
            Product draft = CatalogCsvImportService.parseProduct(row);
            String receiptId = row.isMapped("Receipt ID")
                    ? CsvImportSupport.text(row, "Receipt ID", 100)
                    : requiredDeliveryReference(deliveryReference);
            String quantityColumn = row.isMapped("Units Received") ? "Units Received" : "Initial Units";
            if (row.isMapped("Units Received") && row.isMapped("Initial Units")) {
                throw new IllegalArgumentException("Supply Units Received or Initial Units, not both.");
            }
            int units = CsvImportSupport.integer(row, quantityColumn, 1);
            CatalogCsvImportService.requireUnique(identifiers, receiptId + "::" + draft.getExternalSku());
            return new NewProduct(draft, units, receiptId);
        });
        rows.sort(Comparator.comparing(row -> row.draft().getExternalSku().toLowerCase(java.util.Locale.ROOT)));
        int createdProducts = 0;
        int applied = 0;
        for (NewProduct row : rows) {
            Product product = productRepository.findByExternalSkuIgnoreCase(row.draft().getExternalSku()).orElse(null);
            boolean created = product == null;
            if (created) {
                product = row.draft();
                product.setProductCode(businessCodeGenerator.nextProductCode());
                product = productRepository.saveAndFlush(product);
            }
            if (receiveStock(new Receipt(row.receiptId(), product.getId(), row.units()))) {
                if (created) {
                    createdProducts++;
                } else {
                    CatalogCsvImportService.copyProduct(row.draft(), product);
                    productRepository.save(product);
                }
                applied++;
            }
        }
        return catalogCsvImportService.recordImport(file, "DEPOT_PRODUCTS", rows.size(), createdProducts,
                applied - createdProducts, rows.size() - applied, 0, createdProducts);
    }

    private String requiredDeliveryReference(String value) {
        String reference = value == null ? "" : value.trim();
        if (reference.isEmpty() || reference.length() > 100) {
            throw new IllegalArgumentException("A delivery reference (Receipt ID, 1-100 characters) is required. "
                    + "Enter it on the upload form or add a Receipt ID column to the CSV.");
        }
        return reference;
    }

    public CatalogImportResponse refill(MultipartFile file) {
        var identifiers = new HashSet<String>();
        var rows = CsvImportSupport.read(file, List.of("Receipt ID", "Units Received"), row -> {
            String receiptId = CsvImportSupport.text(row, "Receipt ID", 100);
            boolean useSku = row.isMapped("External SKU");
            if (useSku && row.isMapped("Product ID")) {
                throw new IllegalArgumentException("Supply Product ID or External SKU, not both.");
            }
            String productCode = CsvImportSupport.text(row, useSku ? "External SKU" : "Product ID", useSku ? 100 : 50);
            int units = CsvImportSupport.integer(row, "Units Received", 1);
            CatalogCsvImportService.requireUnique(identifiers, receiptId + "::" + productCode);
            Product product = (useSku ? productRepository.findByExternalSkuIgnoreCase(productCode)
                    : productRepository.findByProductCode(productCode)).orElseThrow(() ->
                    new IllegalArgumentException("Unknown Product ID: " + productCode + ". Add the complete product first."));
            return new Receipt(receiptId, product.getId(), units);
        });
        // Consistent ordering avoids acquiring stock row locks in opposite orders across files.
        rows.sort(Comparator.comparing(Receipt::productId).thenComparing(Receipt::id));
        int applied = 0;
        for (Receipt receipt : rows) {
            if (receiveStock(receipt)) applied++;
        }
        return catalogCsvImportService.recordImport(file, "DEPOT_REFILL", rows.size(), 0, applied,
                rows.size() - applied, 0, 0);
    }

    private boolean receiveStock(Receipt receipt) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO depot_stock_receipts (receipt_id, product_id, units_received)
                VALUES (?, ?, ?) ON CONFLICT (receipt_id, product_id) DO NOTHING
                """, receipt.id(), receipt.productId(), receipt.units());
        if (inserted == 0) {
            Integer previous = jdbcTemplate.queryForObject(
                    "SELECT units_received FROM depot_stock_receipts WHERE receipt_id = ? AND product_id = ?",
                    Integer.class, receipt.id(), receipt.productId());
            if (!Integer.valueOf(receipt.units()).equals(previous)) {
                throw new IllegalArgumentException("Nothing was imported. Receipt " + receipt.id()
                        + " already exists with a different quantity. Use a new receipt ID for a new delivery.");
            }
            return false;
        }
        int updated = jdbcTemplate.update("""
                INSERT INTO depot_inventory (product_id, available_units, reserved_units, last_updated, created_at, updated_at)
                VALUES (?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (product_id) DO UPDATE SET
                    available_units = depot_inventory.available_units + EXCLUDED.available_units,
                    last_updated = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE depot_inventory.available_units::BIGINT + EXCLUDED.available_units <= 2147483647
                """, receipt.productId(), receipt.units());
        if (updated == 0) throw new IllegalArgumentException("Nothing was imported. Receipt " + receipt.id() + " exceeds the stock limit.");
        return true;
    }

    private record NewProduct(Product draft, int units, String receiptId) {}
    private record Receipt(String id, Long productId, int units) {}
}

package com.depotiq.services;

import com.depotiq.dtos.importing.CatalogImportResponse;
import com.depotiq.models.ImportAuditLog;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.models.StoreType;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.StoreRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CatalogCsvImportService {
    static final List<String> STORE_COLUMNS = List.of(
            "External Store ID", "Name", "Store Type", "Region", "Has Warehouse",
            "Storage Capacity", "Delivery Lead Time Days", "Preferred Horizon Days");
    static final List<String> PRODUCT_COLUMNS = List.of(
            "External SKU", "Name", "Category", "Brand", "Supplier Code", "Unit Cost",
            "Price", "Weight Kg", "Shelf Life Days", "Perishable");

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ImportAuditLogRepository importAuditLogRepository;
    private final BusinessCodeGenerator businessCodeGenerator;

    public CatalogCsvImportService(StoreRepository storeRepository, ProductRepository productRepository,
            ImportAuditLogRepository importAuditLogRepository, BusinessCodeGenerator businessCodeGenerator) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.importAuditLogRepository = importAuditLogRepository;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CatalogImportResponse importStores(MultipartFile file) {
        var identifiers = new HashSet<String>();
        var rows = CsvImportSupport.read(file, STORE_COLUMNS, row -> {
            Store draft = parseStore(row);
            requireUnique(identifiers, draft.getExternalStoreId());
            return draft;
        });
        int created = 0;
        for (Store draft : rows) {
            Store store = storeRepository.findByExternalStoreIdIgnoreCase(draft.getExternalStoreId()).orElse(null);
            if (store == null) {
                store = draft;
                store.setStoreCode(businessCodeGenerator.nextStoreCode());
                created++;
            } else {
                store.setName(draft.getName());
                store.setStoreType(draft.getStoreType());
                store.setRegion(draft.getRegion());
                store.setHasWarehouse(draft.getHasWarehouse());
                store.setStorageCapacity(draft.getStorageCapacity());
                store.setDeliveryLeadTimeDays(draft.getDeliveryLeadTimeDays());
                store.setPreferredHorizonDays(draft.getPreferredHorizonDays());
            }
            storeRepository.save(store);
        }
        return recordImport(file, "STORE_CATALOG", rows.size(), created, rows.size() - created, 0, created, 0);
    }

    public CatalogImportResponse importProducts(MultipartFile file) {
        var identifiers = new HashSet<String>();
        var rows = CsvImportSupport.read(file, PRODUCT_COLUMNS, row -> {
            Product draft = parseProduct(row);
            requireUnique(identifiers, draft.getExternalSku());
            return draft;
        });
        int created = 0;
        for (Product draft : rows) {
            Product product = productRepository.findByExternalSkuIgnoreCase(draft.getExternalSku()).orElse(null);
            if (product == null) {
                product = draft;
                product.setProductCode(businessCodeGenerator.nextProductCode());
                created++;
            } else {
                copyProduct(draft, product);
            }
            productRepository.save(product);
        }
        return recordImport(file, "PRODUCT_CATALOG", rows.size(), created, rows.size() - created, 0, 0, created);
    }

    static Product parseProduct(CSVRecord row) {
        Product product = new Product();
        product.setExternalSku(CsvImportSupport.text(row, "External SKU", 100));
        product.setName(CsvImportSupport.text(row, "Name", 150));
        product.setCategory(CsvImportSupport.text(row, "Category", 100));
        product.setBrand(CsvImportSupport.text(row, "Brand", 100));
        product.setSupplierCode(CsvImportSupport.text(row, "Supplier Code", 100));
        product.setUnitCost(CsvImportSupport.decimal(row, "Unit Cost", 12, 2));
        product.setPrice(CsvImportSupport.decimal(row, "Price", 12, 2));
        product.setWeightKg(CsvImportSupport.decimal(row, "Weight Kg", 10, 3));
        if (product.getWeightKg().signum() == 0) throw new IllegalArgumentException("Weight Kg must be greater than zero.");
        product.setPerishable(CsvImportSupport.bool(row, "Perishable"));
        product.setShelfLifeDays(CsvImportSupport.integer(row, "Shelf Life Days", product.getPerishable() ? 1 : 0));
        return product;
    }

    static void copyProduct(Product draft, Product product) {
        product.setName(draft.getName());
        product.setCategory(draft.getCategory());
        product.setBrand(draft.getBrand());
        product.setSupplierCode(draft.getSupplierCode());
        product.setUnitCost(draft.getUnitCost());
        product.setPrice(draft.getPrice());
        product.setWeightKg(draft.getWeightKg());
        product.setShelfLifeDays(draft.getShelfLifeDays());
        product.setPerishable(draft.getPerishable());
    }

    private static Store parseStore(CSVRecord row) {
        Store store = new Store();
        store.setExternalStoreId(CsvImportSupport.text(row, "External Store ID", 100));
        store.setName(CsvImportSupport.text(row, "Name", 150));
        try {
            store.setStoreType(StoreType.valueOf(CsvImportSupport.required(row, "Store Type")
                    .toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_')));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Store Type must be Small, Medium, Large, or Warehouse Store.");
        }
        store.setRegion(CsvImportSupport.text(row, "Region", 100));
        store.setHasWarehouse(CsvImportSupport.bool(row, "Has Warehouse"));
        store.setStorageCapacity(CsvImportSupport.integer(row, "Storage Capacity", 1));
        store.setDeliveryLeadTimeDays(CsvImportSupport.integer(row, "Delivery Lead Time Days", 1));
        store.setPreferredHorizonDays(CsvImportSupport.integer(row, "Preferred Horizon Days", 1));
        if (!List.of(3, 7, 14, 30).contains(store.getPreferredHorizonDays())) {
            throw new IllegalArgumentException("Preferred Horizon Days must be 3, 7, 14, or 30.");
        }
        return store;
    }

    static void requireUnique(java.util.Set<String> identifiers, String identifier) {
        if (!identifiers.add(identifier.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate identifier in this file: " + identifier);
        }
    }

    public CatalogImportResponse recordImport(MultipartFile file, String type, int processed,
            int created, int updated, int skipped, int createdStores, int createdProducts) {
        ImportAuditLog log = new ImportAuditLog();
        log.setFileName(CsvImportSupport.fileName(file));
        log.setImportType(type);
        log.setProcessedRows(processed);
        log.setCreatedRecords(created);
        log.setUpdatedRecords(updated);
        log.setSkippedRows(skipped);
        log.setCreatedStores(createdStores);
        log.setCreatedProducts(createdProducts);
        importAuditLogRepository.save(log);
        return new CatalogImportResponse(type, log.getFileName(), processed, created, updated, skipped, List.of());
    }
}

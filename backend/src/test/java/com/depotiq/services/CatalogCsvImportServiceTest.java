package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.importing.CatalogImportResponse;
import com.depotiq.models.ImportAuditLog;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.models.StoreType;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.StoreRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CatalogCsvImportServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImportAuditLogRepository importAuditLogRepository;

    @Mock
    private BusinessCodeGenerator businessCodeGenerator;

    @Test
    void createsStoreWithGeneratedInternalCode() {
        CatalogCsvImportService service = service();
        when(storeRepository.findByExternalStoreIdIgnoreCase("POS-STORE-42"))
                .thenReturn(Optional.empty());
        when(businessCodeGenerator.nextStoreCode()).thenReturn("S0015");

        CatalogImportResponse response = service.importStores(csv(
                "stores.csv",
                "External Store ID,Name,Store Type,Region,Has Warehouse,Storage Capacity,"
                        + "Delivery Lead Time Days,Preferred Horizon Days\n"
                        + "POS-STORE-42,Airport Market,Warehouse Store,North,yes,8500,3,30\n"
        ));

        ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(storeCaptor.capture());
        Store saved = storeCaptor.getValue();
        assertThat(saved.getStoreCode()).isEqualTo("S0015");
        assertThat(saved.getExternalStoreId()).isEqualTo("POS-STORE-42");
        assertThat(saved.getStoreType()).isEqualTo(StoreType.WAREHOUSE_STORE);
        assertThat(saved.getHasWarehouse()).isTrue();
        assertThat(saved.getPreferredHorizonDays()).isEqualTo(30);
        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.updatedRecords()).isZero();
        assertThat(response.skippedRows()).isZero();
        verify(importAuditLogRepository).save(any(ImportAuditLog.class));
    }

    @Test
    void updatesExistingProductByExternalSkuWithoutReplacingInternalCode() {
        CatalogCsvImportService service = service();
        Product existing = new Product();
        existing.setProductCode("P0042");
        existing.setExternalSku("SKU-100");
        when(productRepository.findByExternalSkuIgnoreCase("SKU-100"))
                .thenReturn(Optional.of(existing));

        CatalogImportResponse response = service.importProducts(csv(
                "products.csv",
                "External SKU,Name,Category,Brand,Unit Cost,Price,Perishable\n"
                        + "SKU-100,Whole Milk 1L,Dairy,Fresh Valley,1.10,1.89,true\n"
        ));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getProductCode()).isEqualTo("P0042");
        assertThat(saved.getName()).isEqualTo("Whole Milk 1L");
        assertThat(saved.getUnitCost()).isEqualByComparingTo(new BigDecimal("1.10"));
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("1.89"));
        assertThat(saved.getPerishable()).isTrue();
        assertThat(response.createdRecords()).isZero();
        assertThat(response.updatedRecords()).isEqualTo(1);
    }

    @Test
    void acceptsProductCsvWithoutOptionalColumns() {
        CatalogCsvImportService service = service();
        when(productRepository.findByExternalSkuIgnoreCase("SKU-200"))
                .thenReturn(Optional.empty());
        when(businessCodeGenerator.nextProductCode()).thenReturn("P0043");

        CatalogImportResponse response = service.importProducts(csv(
                "products.csv",
                "External SKU,Name,Category,Perishable\n"
                        + "SKU-200,Desk Lamp,Household,no\n"
        ));

        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void skipsInvalidRowsAndReportsTheirLineNumbers() {
        CatalogCsvImportService service = service();

        CatalogImportResponse response = service.importStores(csv(
                "stores.csv",
                "External Store ID,Name,Store Type,Region,Has Warehouse,Storage Capacity,"
                        + "Delivery Lead Time Days,Preferred Horizon Days\n"
                        + "POS-STORE-99,Bad Store,Small,North,false,0,2,3\n"
        ));

        assertThat(response.processedRows()).isEqualTo(1);
        assertThat(response.createdRecords()).isZero();
        assertThat(response.skippedRows()).isEqualTo(1);
        assertThat(response.errors()).containsExactly("Line 2: Storage Capacity must be at least 1.");
    }

    @Test
    void rejectsCsvWhenRequiredHeadersAreMissing() {
        CatalogCsvImportService service = service();

        assertThatThrownBy(() -> service.importProducts(csv(
                "products.csv",
                "External SKU,Name,Category\nSKU-1,Milk,Dairy\n"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required columns: Perishable");
    }

    private CatalogCsvImportService service() {
        return new CatalogCsvImportService(
                storeRepository,
                productRepository,
                importAuditLogRepository,
                businessCodeGenerator
        );
    }

    private MockMultipartFile csv(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "text/csv", content.getBytes());
    }
}

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
        when(businessCodeGenerator.nextStoreCode()).thenReturn("S015");

        CatalogImportResponse response = service.importStores(csv(
                "stores.csv",
                "External Store ID,Name,Store Type,Region,Has Warehouse,Storage Capacity,"
                        + "Delivery Lead Time Days,Preferred Horizon Days\n"
                        + "POS-STORE-42,Airport Market,Warehouse Store,North,yes,8500,3,30\n"
        ));

        ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(storeCaptor.capture());
        Store saved = storeCaptor.getValue();
        assertThat(saved.getStoreCode()).isEqualTo("S015");
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
                "External SKU,Name,Category,Brand,Supplier Code,Unit Cost,Price,Weight Kg,Shelf Life Days,Perishable\n"
                        + "SKU-100,Whole Milk 1L,Dairy,Fresh Valley,SUP001,1.10,1.89,1.0,14,true\n"
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
    void rejectsProductCsvWithoutCompleteDetails() {
        assertThatThrownBy(() -> service().importProducts(csv("products.csv",
                "External SKU,Name,Category,Perishable\nSKU-200,Desk Lamp,Household,no\n")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Brand", "Price", "Weight Kg");
        org.mockito.Mockito.verifyNoInteractions(productRepository, businessCodeGenerator);
    }

    @Test
    void rejectsInvalidRowsAndReportsTheirLineNumbers() {
        assertThatThrownBy(() -> service().importStores(csv("stores.csv",
                "External Store ID,Name,Store Type,Region,Has Warehouse,Storage Capacity,"
                        + "Delivery Lead Time Days,Preferred Horizon Days\n"
                        + "POS-STORE-99,Bad Store,Small,North,false,0,2,3\n")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 2: Storage Capacity must be at least 1.");
        org.mockito.Mockito.verifyNoInteractions(storeRepository, businessCodeGenerator);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void rejectsEveryBlankProductFieldBeforeChangingExistingData(int column) {
        String[] values = {"SKU-100", "Milk", "Dairy", "Fresh Valley", "SUP001", "1.10", "1.89", "1", "14", "true"};
        String good = String.join(",", values);
        values[column] = "   ";
        String header = String.join(",", CatalogCsvImportService.PRODUCT_COLUMNS);
        assertThatThrownBy(() -> service().importProducts(csv("products.csv",
                header + "\n" + good + "\n" + String.join(",", values))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Line 3", "required");
        org.mockito.Mockito.verifyNoInteractions(productRepository, businessCodeGenerator, importAuditLogRepository);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void rejectsEveryBlankStoreField(int column) {
        String[] values = {"POS-42", "Market", "Medium", "North", "false", "1200", "2", "7"};
        values[column] = " ";
        assertThatThrownBy(() -> service().importStores(csv("stores.csv",
                String.join(",", CatalogCsvImportService.STORE_COLUMNS) + "\n" + String.join(",", values))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Line 2", "required");
        org.mockito.Mockito.verifyNoInteractions(storeRepository, businessCodeGenerator);
    }

    @Test
    void lateInvalidValueCannotMutateAnExistingManagedProduct() {
        assertThatThrownBy(() -> service().importProducts(csv("products.csv",
                String.join(",", CatalogCsvImportService.PRODUCT_COLUMNS)
                        + "\nSKU-100,Changed,Dairy,Brand,SUP,1,2,1,14,not-a-boolean")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Perishable");
        org.mockito.Mockito.verifyNoInteractions(productRepository, businessCodeGenerator);
    }

    @Test
    void rejectsCsvWhenRequiredHeadersAreMissing() {
        CatalogCsvImportService service = service();

        assertThatThrownBy(() -> service.importProducts(csv(
                "products.csv",
                "External SKU,Name,Category\nSKU-1,Milk,Dairy\n"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required columns:", "Perishable");
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

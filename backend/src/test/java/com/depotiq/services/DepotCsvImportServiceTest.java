package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.depotiq.models.Product;
import com.depotiq.repositories.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DepotCsvImportServiceTest {
    @Mock ProductRepository productRepository;
    @Mock BusinessCodeGenerator businessCodeGenerator;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock CatalogCsvImportService catalogCsvImportService;
    @InjectMocks DepotCsvImportService service;

    @Test
    void rejectsInvalidQuantityBeforeAnyStockOrReceiptWrites() {
        assertThatThrownBy(() -> service.refill(csv("Receipt ID,Product ID,Units Received\nR1,P0001,-1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Units Received");
        verifyNoInteractions(jdbcTemplate, productRepository, catalogCsvImportService);
    }

    @Test
    void rejectsUnknownProductWithoutCreatingIncompleteCatalogData() {
        assertThatThrownBy(() -> service.refill(csv("Receipt ID,Product ID,Units Received\nR1,UNKNOWN,10")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown Product ID");
        verifyNoInteractions(jdbcTemplate, businessCodeGenerator, catalogCsvImportService);
        verify(productRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateReceiptProductRowsBeforeApplyingStock() {
        knownProduct();
        assertThatThrownBy(() -> service.refill(csv("Receipt ID,Product ID,Units Received\nR1,P0001,10\nR1,P0001,10")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate identifier");
        verifyNoInteractions(jdbcTemplate, catalogCsvImportService);
    }

    @Test
    void repeatedReceiptIsSkippedWithoutChangingStock() {
        knownProduct();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("R1"), eq(1L))).thenReturn(10);
        var file = csv("Receipt ID,Product ID,Units Received\nR1,P0001,10");
        service.refill(file);
        verify(jdbcTemplate, times(1)).update(anyString(), eq("R1"), eq(1L), eq(10));
        verify(jdbcTemplate, never()).update(anyString(), eq(1L), anyInt());
        verify(catalogCsvImportService).recordImport(file, "DEPOT_REFILL", 1, 0, 0, 1, 0, 0);
    }

    @Test
    void alteredReceiptQuantityIsRejected() {
        knownProduct();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("R1"), eq(1L))).thenReturn(10);
        assertThatThrownBy(() -> service.refill(csv("Receipt ID,Product ID,Units Received\nR1,P0001,20")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("different quantity");
        verify(jdbcTemplate, never()).update(anyString(), eq(1L), anyInt());
        verifyNoInteractions(catalogCsvImportService);
    }

    @Test
    void productAdditionRequiresFullMetadataAndInitialQuantity() {
        assertThatThrownBy(() -> service.addProducts(csv("External SKU,Name,Initial Units\nSKU,Milk,10")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Missing required columns");
        verifyNoInteractions(jdbcTemplate, productRepository, businessCodeGenerator, catalogCsvImportService);
    }

    @Test
    void combinedUploadRefillsAnExistingProductWithLegacyInitialUnits() {
        Product existing = existingSku();
        when(jdbcTemplate.update(anyString(), eq("DELIVERY-2"), eq(1L), eq(50))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq(1L), eq(50))).thenReturn(1);
        var file = csv(productCsv("Initial Units", "SKU-RICE", "50"));
        service.addProducts(file, "DELIVERY-2");
        verify(jdbcTemplate).update(contains("available_units = depot_inventory.available_units + EXCLUDED.available_units"), eq(1L), eq(50));
        verify(productRepository).save(existing);
        verify(catalogCsvImportService).recordImport(file, "DEPOT_PRODUCTS", 1, 0, 1, 0, 0, 0);
        verifyNoInteractions(businessCodeGenerator);
    }

    @Test
    void combinedUploadCreatesNewProductAndReceivesStock() {
        when(businessCodeGenerator.nextProductCode()).thenReturn("P0042");
        when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0); product.setId(42L); return product;
        });
        when(jdbcTemplate.update(anyString(), eq("DELIVERY-3"), eq(42L), eq(25))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq(42L), eq(25))).thenReturn(1);
        var file = csv(productCsv("Units Received", "SKU-NEW", "25"));
        service.addProducts(file, "DELIVERY-3");
        verify(catalogCsvImportService).recordImport(file, "DEPOT_PRODUCTS", 1, 1, 0, 0, 0, 1);
    }

    @Test
    void repeatedCombinedUploadSkipsStockAndDoesNotOverwriteProductDetails() {
        Product existing = existingSku();
        existing.setName("Current product details");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("DELIVERY-2"), eq(1L))).thenReturn(50);
        var file = csv(productCsv("Units Received", "SKU-RICE", "50"));
        service.addProducts(file, "DELIVERY-2");
        org.assertj.core.api.Assertions.assertThat(existing.getName()).isEqualTo("Current product details");
        verify(productRepository, never()).save(any());
        verify(jdbcTemplate, never()).update(anyString(), eq(1L), anyInt());
        verify(catalogCsvImportService).recordImport(file, "DEPOT_PRODUCTS", 1, 0, 0, 1, 0, 0);
    }

    @Test
    void combinedUploadRequiresADeliveryReferenceBeforeAnyMutation() {
        assertThatThrownBy(() -> service.addProducts(csv(productCsv("Initial Units", "SKU-RICE", "50")), "   "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delivery reference");
        verifyNoInteractions(productRepository, jdbcTemplate, catalogCsvImportService);
    }

    @Test
    void refillCanMatchExternalSkuInsteadOfAnInternalCode() {
        existingSku();
        when(jdbcTemplate.update(anyString(), eq("R-SKU"), eq(1L), eq(10))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq(1L), eq(10))).thenReturn(1);
        var file = csv("Receipt ID,External SKU,Units Received\nR-SKU,SKU-RICE,10");
        service.refill(file);
        verify(catalogCsvImportService).recordImport(file, "DEPOT_REFILL", 1, 0, 1, 0, 0, 0);
    }

    @Test
    void mixedFileCanCreateAndRefillTogether() {
        existingSku();
        doReturn(Optional.empty()).when(productRepository).findByExternalSkuIgnoreCase("SKU-NEW");
        when(businessCodeGenerator.nextProductCode()).thenReturn("P0042");
        when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0); product.setId(42L); return product;
        });
        when(jdbcTemplate.update(anyString(), eq("MIXED"), anyLong(), eq(50))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), anyLong(), eq(50))).thenReturn(1);
        String csvText = productCsv("Units Received", "SKU-RICE", "50") + "\n"
                + productCsv("Units Received", "SKU-NEW", "50").split("\n")[1];
        var file = csv(csvText);
        service.addProducts(file, "MIXED");
        verify(catalogCsvImportService).recordImport(file, "DEPOT_PRODUCTS", 2, 1, 1, 0, 0, 1);
    }

    private Product existingSku() {
        Product product = new Product(); product.setId(1L); product.setProductCode("P0041"); product.setExternalSku("SKU-RICE");
        when(productRepository.findByExternalSkuIgnoreCase("SKU-RICE")).thenReturn(Optional.of(product));
        return product;
    }

    private String productCsv(String quantityColumn, String sku, String quantity) {
        return String.join(",", CatalogCsvImportService.PRODUCT_COLUMNS) + "," + quantityColumn + "\n"
                + sku + ",Rice,Groceries,Brand,SUP,1,2,1,365,false," + quantity;
    }

    private void knownProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setProductCode("P0001");
        when(productRepository.findByProductCode("P0001")).thenReturn(Optional.of(product));
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "stock.csv", "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

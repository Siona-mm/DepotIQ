package com.depotiq.services;

import com.depotiq.dtos.product.CreateProductRequest;
import com.depotiq.dtos.product.UpdateProductRequest;
import com.depotiq.mappers.ProductMapper;
import com.depotiq.models.Product;
import com.depotiq.repositories.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @Mock BusinessCodeGenerator codes;
    private ProductService service() { return new ProductService(repository, new ProductMapper(), codes); }

    @Test
    void createPersistsAllSuppliedDetailsAndTrimsText() {
        var request = new CreateProductRequest();
        request.setName(" Rice "); request.setCategory(" Food "); request.setBrand(" Grain Co ");
        request.setSupplierCode(" SUP-1 "); request.setExternalSku(" RICE-5KG ");
        request.setUnitCost(new BigDecimal("3.25")); request.setPrice(new BigDecimal("5.95"));
        request.setWeightKg(new BigDecimal("5.001")); request.setShelfLifeDays(90); request.setPerishable(true);
        when(codes.nextProductCode()).thenReturn("P0042");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service().createProduct(request);
        assertThat(result).extracting("productCode", "name", "category", "brand", "supplierCode", "externalSku",
                "unitCost", "price", "weightKg", "shelfLifeDays", "perishable")
                .containsExactly("P0042", "Rice", "Food", "Grain Co", "SUP-1", "RICE-5KG",
                        new BigDecimal("3.25"), new BigDecimal("5.95"), new BigDecimal("5.001"), 90, true);
    }

    @Test
    void editRetainsInternalIdentityAndSavesFullMetadata() {
        var product = existing(1L); product.setExternalSku("RICE-5KG");
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.findByExternalSkuIgnoreCase("RICE-5KG")).thenReturn(Optional.of(product));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var request = update();
        var result = service().updateProduct(1L, request);
        assertThat(result).extracting("id", "productCode", "name", "category", "brand", "supplierCode", "externalSku",
                "unitCost", "price", "weightKg", "shelfLifeDays", "perishable")
                .containsExactly(1L, "P0041", "Rice", "Food", "Grain Co", "SUP-1", "RICE-5KG",
                        new BigDecimal("3.25"), new BigDecimal("5.95"), new BigDecimal("5.001"), 90, true);
        verifyNoInteractions(codes);
    }

    @Test
    void duplicateSkuCannotOverwriteAnotherProductOrConsumeACode() {
        var product = existing(1L); product.setName("Unchanged");
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.findByExternalSkuIgnoreCase("RICE-5KG")).thenReturn(Optional.of(existing(2L)));
        assertThatThrownBy(() -> service().updateProduct(1L, update())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(product.getName()).isEqualTo("Unchanged");
        var create = new CreateProductRequest(); create.setExternalSku(" RICE-5KG ");
        assertThatThrownBy(() -> service().createProduct(create)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("already assigned");
        verify(repository, never()).save(any()); verifyNoInteractions(codes);
    }

    private Product existing(Long id) { var product = new Product(); product.setId(id); product.setProductCode("P0041"); return product; }
    private UpdateProductRequest update() {
        var request = new UpdateProductRequest(); request.setName(" Rice "); request.setCategory(" Food ");
        request.setBrand(" Grain Co "); request.setSupplierCode(" SUP-1 "); request.setExternalSku(" RICE-5KG ");
        request.setUnitCost(new BigDecimal("3.25")); request.setPrice(new BigDecimal("5.95"));
        request.setWeightKg(new BigDecimal("5.001")); request.setShelfLifeDays(90); request.setPerishable(true);
        return request;
    }
}

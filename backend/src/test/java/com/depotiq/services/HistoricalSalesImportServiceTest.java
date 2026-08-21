package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.StoreRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class HistoricalSalesImportServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImportAuditLogRepository importAuditLogRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HistoricalSalesImportService service;

    @Test
    void importsKnownStoreAndProductRows() {
        Store store = new Store();
        store.setId(1L);
        store.setStoreCode("S001");
        store.setRegion("Old region");
        Product product = new Product();
        product.setId(2L);
        product.setProductCode("P0001");
        product.setCategory("Other");
        product.setPrice(BigDecimal.ONE);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findAll()).thenReturn(List.of(product));
        mockExistingSalesKeys();

        HistoricalSalesImportResponse response = service.importCsv(csvFile(
                "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn"
        ));

        verify(jdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
    }

    @Test
    void createsMissingStoreAndProductFromCsvMetadata() {
        when(storeRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAll()).thenReturn(List.of());
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            store.setId(99L);
            return store;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(999L);
            return product;
        });
        mockExistingSalesKeys();

        HistoricalSalesImportResponse response = service.importCsv(csvFile(
                "2022-01-01,S999,P9999,Electronics,South,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn"
        ));

        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.createdStores()).isEqualTo(1);
        assertThat(response.createdProducts()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
        verify(storeRepository).save(any(Store.class));
        verify(productRepository).save(any(Product.class));
        verify(jdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
    }

    @Test
    void supportsExternalIdentifiersAndQuotedCommaValues() {
        Store store = new Store();
        store.setId(1L);
        store.setStoreCode("S001");
        Product product = new Product();
        product.setId(2L);
        product.setProductCode("P0001");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findAll()).thenReturn(List.of(product));
        mockExistingSalesKeys();

        HistoricalSalesImportResponse response = service.importCsv(csvFileWithExternalColumns(
                "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,\"Rainy, windy\",0,29.69,Autumn,POS_DEMO,SALE-1001"
        ));

        assertThat(response.createdRecords()).isEqualTo(1);
        verify(jdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
    }

    private MockMultipartFile csvFile(String row) {
        String content = "Date,Store ID,Product ID,Category,Region,Inventory Level,Units Sold,Units Ordered,"
                + "Demand Forecast,Price,Discount,Weather Condition,Holiday/Promotion,Competitor Pricing,Seasonality\n"
                + row;
        return new MockMultipartFile("file", "sales.csv", "text/csv", content.getBytes());
    }

    private MockMultipartFile csvFileWithExternalColumns(String row) {
        String content = "Date,Store ID,Product ID,Category,Region,Inventory Level,Units Sold,Units Ordered,"
                + "Demand Forecast,Price,Discount,Weather Condition,Holiday/Promotion,Competitor Pricing,Seasonality,"
                + "Source System,External Record ID\n"
                + row;
        return new MockMultipartFile("file", "external-sales.csv", "text/csv", content.getBytes());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mockExistingSalesKeys() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
    }
}

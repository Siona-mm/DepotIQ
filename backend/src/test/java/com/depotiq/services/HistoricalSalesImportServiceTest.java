package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.models.Product;
import com.depotiq.models.SalesRecord;
import com.depotiq.models.Store;
import com.depotiq.models.StoreInventory;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.ImportAuditLogRepository;
import com.depotiq.repositories.SalesRecordRepository;
import com.depotiq.repositories.StoreRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class HistoricalSalesImportServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesRecordRepository salesRecordRepository;

    @Mock
    private StoreInventoryRepository storeInventoryRepository;

    @Mock
    private ImportAuditLogRepository importAuditLogRepository;

    @Captor
    private ArgumentCaptor<SalesRecord> salesRecordCaptor;

    @Captor
    private ArgumentCaptor<StoreInventory> storeInventoryCaptor;

    @InjectMocks
    private HistoricalSalesImportService service;

    @Test
    void importsKnownStoreAndProductRows() {
        Store store = new Store();
        store.setId(1L);
        store.setRegion("Old region");
        Product product = new Product();
        product.setId(2L);
        product.setCategory("Other");
        product.setPrice(BigDecimal.ONE);
        when(storeRepository.findByStoreCode("S001")).thenReturn(Optional.of(store));
        when(productRepository.findByProductCode("P0001")).thenReturn(Optional.of(product));
        when(salesRecordRepository.findBySourceSystemAndExternalRecordId(any(), any()))
                .thenReturn(Optional.empty());
        when(salesRecordRepository.findByStoreIdAndProductIdAndSaleDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(storeInventoryRepository.findByStoreIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());

        HistoricalSalesImportResponse response = service.importCsv(csvFile(
                "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn"
        ));

        verify(salesRecordRepository).save(salesRecordCaptor.capture());
        verify(storeInventoryRepository).save(storeInventoryCaptor.capture());
        SalesRecord saved = salesRecordCaptor.getValue();
        StoreInventory inventory = storeInventoryCaptor.getValue();
        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
        assertThat(saved.getUnitsSold()).isEqualTo(127);
        assertThat(saved.getPrice()).isEqualByComparingTo("33.5");
        assertThat(saved.getHolidayPromotion()).isFalse();
        assertThat(saved.getSourceSystem()).isEqualTo("CSV_UPLOAD");
        assertThat(saved.getExternalRecordId()).isEqualTo("S001-P0001-2022-01-01");
        assertThat(saved.getImportedAt()).isNotNull();
        assertThat(inventory.getInventoryLevel()).isEqualTo(231);
        assertThat(inventory.getIncomingUnits()).isEqualTo(55);
        assertThat(store.getRegion()).isEqualTo("North");
        assertThat(product.getCategory()).isEqualTo("Groceries");
        assertThat(product.getPrice()).isEqualByComparingTo("33.5");
    }

    @Test
    void createsMissingStoreAndProductFromCsvMetadata() {
        when(storeRepository.findByStoreCode("S999")).thenReturn(Optional.empty());
        when(productRepository.findByProductCode("P9999")).thenReturn(Optional.empty());
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
        when(salesRecordRepository.findBySourceSystemAndExternalRecordId(any(), any()))
                .thenReturn(Optional.empty());
        when(salesRecordRepository.findByStoreIdAndProductIdAndSaleDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(storeInventoryRepository.findByStoreIdAndProductId(99L, 999L))
                .thenReturn(Optional.empty());

        HistoricalSalesImportResponse response = service.importCsv(csvFile(
                "2022-01-01,S999,P9999,Electronics,South,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn"
        ));

        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.createdStores()).isEqualTo(1);
        assertThat(response.createdProducts()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
        verify(storeRepository).save(any(Store.class));
        verify(productRepository).save(any(Product.class));
        verify(storeInventoryRepository).save(any(StoreInventory.class));
    }

    @Test
    void supportsExternalIdentifiersAndQuotedCommaValues() {
        Store store = new Store();
        store.setId(1L);
        Product product = new Product();
        product.setId(2L);
        when(storeRepository.findByStoreCode("S001")).thenReturn(Optional.of(store));
        when(productRepository.findByProductCode("P0001")).thenReturn(Optional.of(product));
        when(salesRecordRepository.findBySourceSystemAndExternalRecordId("POS_DEMO", "SALE-1001"))
                .thenReturn(Optional.empty());
        when(salesRecordRepository.findByStoreIdAndProductIdAndSaleDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(storeInventoryRepository.findByStoreIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());

        HistoricalSalesImportResponse response = service.importCsv(csvFileWithExternalColumns(
                "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,\"Rainy, windy\",0,29.69,Autumn,POS_DEMO,SALE-1001"
        ));

        verify(salesRecordRepository).save(salesRecordCaptor.capture());
        SalesRecord saved = salesRecordCaptor.getValue();
        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(saved.getWeatherCondition()).isEqualTo("Rainy, windy");
        assertThat(saved.getSourceSystem()).isEqualTo("POS_DEMO");
        assertThat(saved.getExternalRecordId()).isEqualTo("SALE-1001");
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
}

package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.importing.HistoricalSalesImportResponse;
import com.depotiq.events.OperationalDataImportedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        verify(eventPublisher).publishEvent(any(OperationalDataImportedEvent.class));
        assertThat(response.createdRecords()).isEqualTo(1);
        assertThat(response.skippedRows()).isZero();
        assertThat(response.importedInventoryKeys()).containsExactly("S001::P0001");
        assertThat(response.planningRefreshRequested()).isTrue();
    }

    @Test
    void rejectsUnknownCatalogIdsWithoutCreatingPlaceholders() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importCsv(csvFile(
                "2022-01-01,S999,P9999,Electronics,South,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown Store ID", "catalog first");
        verify(storeRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate, eventPublisher);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 14})
    void rejectsEachBlankRequiredFieldWithoutSideEffects(int column) {
        String[] values = "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn".split(",");
        values[column] = " ";
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importCsv(csvFile(String.join(",", values))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Line 2", "required");
        verify(storeRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate, eventPublisher, importAuditLogRepository);
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

    @Test
    void rejectsNonCsvFilesBeforeReadingRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.txt",
                "text/plain",
                "not,csv".getBytes()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only CSV files are supported.");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void doesNotRefreshPlanningWhenRowsAreInvalid() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importCsv(csvFile(
                "bad-date,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("invalid Date");
        org.mockito.Mockito.verifyNoInteractions(eventPublisher, jdbcTemplate, importAuditLogRepository);
    }

    @Test
    void validatesTheWholeFileBeforeWritingEvenWhenAnEarlierRowIsValid() {
        Store store = new Store(); store.setId(1L); store.setStoreCode("S001");
        Product product = new Product(); product.setId(2L); product.setProductCode("P0001");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findAll()).thenReturn(List.of(product));
        String valid = "2022-01-01,S001,P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn";
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importCsv(csvFile(valid + "\n" + valid.replace(",127,", ",-1,"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Line 3", "Units Sold");
        org.mockito.Mockito.verifyNoInteractions(eventPublisher, jdbcTemplate, importAuditLogRepository);
        verify(storeRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"S0011", "S11", "s011"})
    void legacyStorePaddingMapsToTheSameStore(String inputCode) {
        Store store = new Store(); store.setId(15L); store.setStoreCode("S011");
        Product product = new Product(); product.setId(2L); product.setProductCode("P0001");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findAll()).thenReturn(List.of(product));
        mockExistingSalesKeys();
        var response = service.importCsv(csvFile("2022-01-01," + inputCode
                + ",P0001,Groceries,North,231,127,55,135.47,33.5,20,Rainy,0,29.69,Autumn"));
        assertThat(response.importedInventoryKeys()).containsExactly("S011::P0001");
        verify(storeRepository, never()).save(any());
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

package com.depotiq.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.depotiq.dtos.store.CreateStoreRequest;
import com.depotiq.dtos.store.UpdateStoreRequest;
import com.depotiq.mappers.StoreMapper;
import com.depotiq.models.Store;
import com.depotiq.models.StoreType;
import com.depotiq.repositories.StoreRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {
    @Mock StoreRepository repository;
    @Mock BusinessCodeGenerator codes;
    private StoreService service() { return new StoreService(repository, new StoreMapper(), codes); }

    @Test
    void directoryIsOrderedNumerically() {
        when(repository.findAll()).thenReturn(List.of(store(3L, "S1000"), store(2L, "S999"), store(1L, "S011")));
        assertThat(service().getAllStores()).extracting("storeCode").containsExactly("S011", "S999", "S1000");
    }

    @Test
    void editsExternalIdWithoutChangingInternalStoreIdentity() {
        Store existing = store(1L, "S011");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service().updateStore(1L, update(" POS-NORTH "));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStoreCode()).isEqualTo("S011");
        assertThat(result.getExternalStoreId()).isEqualTo("POS-NORTH");
        assertThat(result.getName()).isEqualTo("North Market");
        assertThat(result.getRegion()).isEqualTo("North");
    }

    @Test
    void rejectsDuplicateExternalIdBeforeMutatingStore() {
        Store existing = store(1L, "S011"); existing.setName("Unchanged");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByExternalStoreIdIgnoreCase("POS-NORTH")).thenReturn(Optional.of(store(2L, "S012")));
        assertThatThrownBy(() -> service().updateStore(1L, update("POS-NORTH")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("already assigned");
        assertThat(existing.getName()).isEqualTo("Unchanged");
        verify(repository, never()).save(any());
    }

    @Test
    void missingExternalIdInOlderClientsDoesNotEraseExistingMapping() {
        Store existing = store(1L, "S011"); existing.setExternalStoreId("POS-NORTH");
        new StoreMapper().updateEntity(existing, update(null));
        assertThat(existing.getExternalStoreId()).isEqualTo("POS-NORTH");
        new StoreMapper().updateEntity(existing, update(""));
        assertThat(existing.getExternalStoreId()).isNull();
    }

    @Test
    void manualCreateAndUpdateRejectUnsupportedForecastHorizons() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var create = new CreateStoreRequest(); create.setPreferredHorizonDays(2);
            var edit = update("POS"); edit.setPreferredHorizonDays(2);
            assertThat(validator.validateProperty(create, "preferredHorizonSupported")).isNotEmpty();
            assertThat(validator.validateProperty(edit, "preferredHorizonSupported")).isNotEmpty();
            for (int horizon : List.of(3, 7, 14, 30)) {
                create.setPreferredHorizonDays(horizon); edit.setPreferredHorizonDays(horizon);
                assertThat(validator.validateProperty(create, "preferredHorizonSupported")).isEmpty();
                assertThat(validator.validateProperty(edit, "preferredHorizonSupported")).isEmpty();
            }
        }
    }

    private Store store(Long id, String code) { Store store = new Store(); store.setId(id); store.setStoreCode(code); return store; }
    private UpdateStoreRequest update(String externalId) {
        var request = new UpdateStoreRequest(); request.setExternalStoreId(externalId); request.setName(" North Market ");
        request.setRegion(" North "); request.setStoreType(StoreType.MEDIUM); request.setHasWarehouse(false);
        request.setStorageCapacity(1200); request.setDeliveryLeadTimeDays(2); request.setPreferredHorizonDays(7);
        return request;
    }
}

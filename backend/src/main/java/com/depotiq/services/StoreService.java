package com.depotiq.services;

import com.depotiq.dtos.store.CreateStoreRequest;
import com.depotiq.dtos.store.StoreResponse;
import com.depotiq.dtos.store.UpdateStoreRequest;
import com.depotiq.mappers.StoreMapper;
import com.depotiq.models.Store;
import com.depotiq.repositories.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final BusinessCodeGenerator businessCodeGenerator;

    public StoreService(
            StoreRepository storeRepository,
            StoreMapper storeMapper,
            BusinessCodeGenerator businessCodeGenerator
    ) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .sorted((left, right) -> BusinessCodes.compareStoreCodes(left.getStoreCode(), right.getStoreCode()))
                .map(storeMapper::toResponse)
                .toList();
    }

    public StoreResponse getStoreById(Long id) {
        Store store = findStoreOrThrow(id);
        return storeMapper.toResponse(store);
    }

    public StoreResponse createStore(CreateStoreRequest request) {
        validateExternalStoreId(request.getExternalStoreId(), null);
        Store store = storeMapper.toEntity(
                request,
                businessCodeGenerator.nextStoreCode()
        );
        Store savedStore = storeRepository.save(store);

        return storeMapper.toResponse(savedStore);
    }

    public StoreResponse updateStore(Long id, UpdateStoreRequest request) {
        Store store = findStoreOrThrow(id);

        validateExternalStoreId(request.getExternalStoreId(), id);
        storeMapper.updateEntity(store, request);
        Store savedStore = storeRepository.save(store);

        return storeMapper.toResponse(savedStore);
    }

    public void deleteStore(Long id) {
        Store store = findStoreOrThrow(id);
        storeRepository.delete(store);
    }

    private void validateExternalStoreId(String externalId, Long currentId) {
        if (externalId == null || externalId.isBlank()) return;
        storeRepository.findByExternalStoreIdIgnoreCase(externalId.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "External Store ID is already assigned to " + existing.getStoreCode());
            }
        });
    }

    private Store findStoreOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    }
}

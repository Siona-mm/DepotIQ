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

    public StoreService(StoreRepository storeRepository, StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
    }

    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(storeMapper::toResponse)
                .toList();
    }

    public StoreResponse getStoreById(Long id) {
        Store store = findStoreOrThrow(id);
        return storeMapper.toResponse(store);
    }

    public StoreResponse createStore(CreateStoreRequest request) {
        if (storeRepository.existsByStoreCode(request.getStoreCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Store code already exists");
        }

        Store store = storeMapper.toEntity(request);
        Store savedStore = storeRepository.save(store);

        return storeMapper.toResponse(savedStore);
    }

    public StoreResponse updateStore(Long id, UpdateStoreRequest request) {
        Store store = findStoreOrThrow(id);

        storeMapper.updateEntity(store, request);
        Store savedStore = storeRepository.save(store);

        return storeMapper.toResponse(savedStore);
    }

    public void deleteStore(Long id) {
        Store store = findStoreOrThrow(id);
        storeRepository.delete(store);
    }

    private Store findStoreOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    }
}

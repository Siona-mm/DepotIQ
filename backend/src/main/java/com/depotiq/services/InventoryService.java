package com.depotiq.services;

import com.depotiq.dtos.inventory.DepotInventoryResponse;
import com.depotiq.dtos.inventory.StoreInventoryResponse;
import com.depotiq.dtos.inventory.UpsertDepotInventoryRequest;
import com.depotiq.dtos.inventory.UpsertStoreInventoryRequest;
import com.depotiq.mappers.InventoryMapper;
import com.depotiq.models.DepotInventory;
import com.depotiq.models.Product;
import com.depotiq.models.Store;
import com.depotiq.models.StoreInventory;
import com.depotiq.repositories.DepotInventoryRepository;
import com.depotiq.repositories.ProductRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import com.depotiq.repositories.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InventoryService {
    private final StoreInventoryRepository storeInventoryRepository;
    private final DepotInventoryRepository depotInventoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(
            StoreInventoryRepository storeInventoryRepository,
            DepotInventoryRepository depotInventoryRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            InventoryMapper inventoryMapper
    ) {
        this.storeInventoryRepository = storeInventoryRepository;
        this.depotInventoryRepository = depotInventoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.inventoryMapper = inventoryMapper;
    }

    public List<StoreInventoryResponse> getAllStoreInventory() {
        return storeInventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toStoreInventoryResponse)
                .toList();
    }

    public List<StoreInventoryResponse> getStoreInventoryByStore(Long storeId) {
        return storeInventoryRepository.findByStoreId(storeId)
                .stream()
                .map(inventoryMapper::toStoreInventoryResponse)
                .toList();
    }

    public StoreInventoryResponse upsertStoreInventory(UpsertStoreInventoryRequest request) {
        Store store = findStoreOrThrow(request.getStoreId());
        Product product = findProductOrThrow(request.getProductId());

        StoreInventory inventory = storeInventoryRepository
                .findByStoreIdAndProductId(request.getStoreId(), request.getProductId())
                .orElseGet(StoreInventory::new);

        inventory.setStore(store);
        inventory.setProduct(product);
        inventory.setInventoryLevel(request.getInventoryLevel());
        inventory.setIncomingUnits(request.getIncomingUnits());
        inventory.setLastUpdated(LocalDateTime.now());

        return inventoryMapper.toStoreInventoryResponse(storeInventoryRepository.save(inventory));
    }

    public void deleteStoreInventory(Long id) {
        StoreInventory inventory = storeInventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store inventory record not found"));
        storeInventoryRepository.delete(inventory);
    }

    public List<DepotInventoryResponse> getAllDepotInventory() {
        return depotInventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toDepotInventoryResponse)
                .toList();
    }

    public DepotInventoryResponse upsertDepotInventory(UpsertDepotInventoryRequest request) {
        Product product = findProductOrThrow(request.getProductId());

        DepotInventory inventory = depotInventoryRepository
                .findByProductId(request.getProductId())
                .orElseGet(DepotInventory::new);

        inventory.setProduct(product);
        inventory.setAvailableUnits(request.getAvailableUnits());
        inventory.setReservedUnits(request.getReservedUnits());
        inventory.setLastUpdated(LocalDateTime.now());

        return inventoryMapper.toDepotInventoryResponse(depotInventoryRepository.save(inventory));
    }

    public void deleteDepotInventory(Long id) {
        DepotInventory inventory = depotInventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Depot inventory record not found"));
        depotInventoryRepository.delete(inventory);
    }

    private Store findStoreOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}

package com.depotiq.controllers;

import com.depotiq.dtos.inventory.DepotInventoryResponse;
import com.depotiq.dtos.inventory.StoreInventoryResponse;
import com.depotiq.dtos.inventory.UpsertDepotInventoryRequest;
import com.depotiq.dtos.inventory.UpsertStoreInventoryRequest;
import com.depotiq.services.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stores")
    public List<StoreInventoryResponse> getAllStoreInventory() {
        return inventoryService.getAllStoreInventory();
    }

    @GetMapping("/stores/{storeId}")
    public List<StoreInventoryResponse> getStoreInventoryByStore(@PathVariable Long storeId) {
        return inventoryService.getStoreInventoryByStore(storeId);
    }

    @PostMapping("/stores")
    public StoreInventoryResponse upsertStoreInventory(@Valid @RequestBody UpsertStoreInventoryRequest request) {
        return inventoryService.upsertStoreInventory(request);
    }

    @DeleteMapping("/stores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoreInventory(@PathVariable Long id) {
        inventoryService.deleteStoreInventory(id);
    }

    @GetMapping("/depot")
    public List<DepotInventoryResponse> getAllDepotInventory() {
        return inventoryService.getAllDepotInventory();
    }

    @PostMapping("/depot")
    public DepotInventoryResponse upsertDepotInventory(@Valid @RequestBody UpsertDepotInventoryRequest request) {
        return inventoryService.upsertDepotInventory(request);
    }

    @DeleteMapping("/depot/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepotInventory(@PathVariable Long id) {
        inventoryService.deleteDepotInventory(id);
    }
}

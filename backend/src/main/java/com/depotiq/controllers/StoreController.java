package com.depotiq.controllers;

import com.depotiq.dtos.store.CreateStoreRequest;
import com.depotiq.dtos.store.StoreResponse;
import com.depotiq.dtos.store.UpdateStoreRequest;
import com.depotiq.services.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreResponse> getAllStores() {
        return storeService.getAllStores();
    }

    @GetMapping("/{id}")
    public StoreResponse getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(@Valid @RequestBody CreateStoreRequest request) {
        return storeService.createStore(request);
    }

    @PutMapping("/{id}")
    public StoreResponse updateStore(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoreRequest request
    ) {
        return storeService.updateStore(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
    }
}

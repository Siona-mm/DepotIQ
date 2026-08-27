package com.depotiq.repositories;

import com.depotiq.models.Store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreCode(String storeCode);

    Optional<Store> findByExternalStoreIdIgnoreCase(String externalStoreId);

    boolean existsByStoreCode(String storeCode);
}

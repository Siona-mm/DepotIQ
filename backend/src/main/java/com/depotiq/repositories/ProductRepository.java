package com.depotiq.repositories;

import com.depotiq.models.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findByExternalSkuIgnoreCase(String externalSku);

    boolean existsByProductCode(String productCode);
}

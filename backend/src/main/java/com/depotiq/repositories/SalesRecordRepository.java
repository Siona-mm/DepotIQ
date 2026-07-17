package com.depotiq.repositories;

import com.depotiq.models.SalesRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {
    List<SalesRecord> findByStoreIdAndProductIdOrderBySaleDateAsc(Long storeId, Long productId);

    Optional<SalesRecord> findByStoreIdAndProductIdAndSaleDate(
        Long storeId,
        Long productId,
        LocalDate saleDate
    );
}

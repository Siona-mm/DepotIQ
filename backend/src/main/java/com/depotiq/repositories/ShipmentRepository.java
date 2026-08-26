package com.depotiq.repositories;

import com.depotiq.models.Shipment;
import com.depotiq.models.ShipmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from Shipment shipment where shipment.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") Long id);

    List<Shipment> findByStatusOrderByPlannedDispatchDateAsc(ShipmentStatus status);

    List<Shipment> findByStoreIdOrderByPlannedDispatchDateDesc(Long storeId);

    List<Shipment> findByStoreIdAndStatusOrderByPlannedDispatchDateAsc(
            Long storeId,
            ShipmentStatus status
    );
}

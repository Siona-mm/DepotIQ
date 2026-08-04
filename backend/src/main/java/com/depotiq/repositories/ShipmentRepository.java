package com.depotiq.repositories;

import com.depotiq.models.Shipment;
import com.depotiq.models.ShipmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByStatusOrderByPlannedDispatchDateAsc(ShipmentStatus status);

    List<Shipment> findByStoreIdOrderByPlannedDispatchDateDesc(Long storeId);

    List<Shipment> findByStoreIdAndStatusOrderByPlannedDispatchDateAsc(
            Long storeId,
            ShipmentStatus status
    );
}

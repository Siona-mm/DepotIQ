package com.depotiq.dtos.shipment;

import com.depotiq.models.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateShipmentStatusRequest {

    @NotNull
    private ShipmentStatus status;

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
}

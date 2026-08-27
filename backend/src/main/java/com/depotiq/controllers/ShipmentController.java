package com.depotiq.controllers;

import com.depotiq.dtos.shipment.CreateShipmentRequest;
import com.depotiq.dtos.shipment.ApproveAndDispatchShipmentRequest;
import com.depotiq.dtos.shipment.ShipmentResponse;
import com.depotiq.dtos.shipment.UpdateShipmentStatusRequest;
import com.depotiq.models.ShipmentStatus;
import com.depotiq.services.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<ShipmentResponse> getShipments(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) ShipmentStatus status
    ) {
        return shipmentService.getShipments(storeId, status);
    }

    @GetMapping("/{id}")
    public ShipmentResponse getShipmentById(@PathVariable Long id) {
        return shipmentService.getShipmentById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(
            @Valid @RequestBody CreateShipmentRequest request
    ) {
        return shipmentService.createShipment(request);
    }

    @PostMapping("/approve-and-dispatch")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse approveAndDispatchShipment(
            @Valid @RequestBody ApproveAndDispatchShipmentRequest request
    ) {
        return shipmentService.approveAndDispatch(request);
    }

    @PatchMapping("/{id}/status")
    public ShipmentResponse updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentStatusRequest request
    ) {
        return shipmentService.updateStatus(id, request);
    }
}

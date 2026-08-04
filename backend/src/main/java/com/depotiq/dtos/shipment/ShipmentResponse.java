package com.depotiq.dtos.shipment;

import com.depotiq.models.ShipmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ShipmentResponse {
    private Long id;
    private String shipmentNumber;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private ShipmentStatus status;
    private LocalDate plannedDispatchDate;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private String notes;
    private Integer totalUnits;
    private List<ShipmentItemResponse> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public LocalDate getPlannedDispatchDate() {
        return plannedDispatchDate;
    }

    public void setPlannedDispatchDate(LocalDate plannedDispatchDate) {
        this.plannedDispatchDate = plannedDispatchDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public List<ShipmentItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItemResponse> items) {
        this.items = items;
    }
}

package com.depotiq.mappers;

import com.depotiq.dtos.shipment.ShipmentItemResponse;
import com.depotiq.dtos.shipment.ShipmentResponse;
import com.depotiq.models.Product;
import com.depotiq.models.Shipment;
import com.depotiq.models.ShipmentItem;
import com.depotiq.models.Store;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        Store store = shipment.getStore();
        ShipmentResponse response = new ShipmentResponse();
        List<ShipmentItemResponse> items = shipment.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        response.setId(shipment.getId());
        response.setShipmentNumber(shipment.getShipmentNumber());
        response.setStoreId(store.getId());
        response.setStoreCode(store.getStoreCode());
        response.setStoreName(store.getName());
        response.setStatus(shipment.getStatus());
        response.setPlannedDispatchDate(shipment.getPlannedDispatchDate());
        response.setExpectedDeliveryDate(shipment.getExpectedDeliveryDate());
        response.setDispatchedAt(shipment.getDispatchedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        response.setNotes(shipment.getNotes());
        response.setItems(items);
        response.setTotalUnits(
                items.stream()
                        .mapToInt(ShipmentItemResponse::getQuantity)
                        .sum()
        );

        return response;
    }

    private ShipmentItemResponse toItemResponse(ShipmentItem item) {
        Product product = item.getProduct();
        ShipmentItemResponse response = new ShipmentItemResponse();

        response.setId(item.getId());
        response.setRecommendationId(item.getRecommendation().getId());
        response.setProductId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getName());
        response.setCategory(product.getCategory());
        response.setQuantity(item.getQuantity());

        return response;
    }
}

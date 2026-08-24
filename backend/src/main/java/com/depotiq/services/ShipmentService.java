package com.depotiq.services;

import com.depotiq.dtos.shipment.CreateShipmentRequest;
import com.depotiq.dtos.shipment.ShipmentResponse;
import com.depotiq.dtos.shipment.UpdateShipmentStatusRequest;
import com.depotiq.mappers.ShipmentMapper;
import com.depotiq.models.DepotInventory;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.models.Shipment;
import com.depotiq.models.ShipmentItem;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.models.ShipmentStatus;
import com.depotiq.models.StoreInventory;
import com.depotiq.repositories.DepotInventoryRepository;
import com.depotiq.repositories.ShipmentItemRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import com.depotiq.repositories.ShipmentRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentRecommendationRepository recommendationRepository;
    private final DepotInventoryRepository depotInventoryRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ShipmentMapper shipmentMapper;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final OperationalActivityService activityService;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            ShipmentItemRepository shipmentItemRepository,
            ShipmentRecommendationRepository recommendationRepository,
            DepotInventoryRepository depotInventoryRepository,
            StoreInventoryRepository storeInventoryRepository,
            ShipmentMapper shipmentMapper,
            BusinessCodeGenerator businessCodeGenerator
    ) {
        this(shipmentRepository, shipmentItemRepository, recommendationRepository, depotInventoryRepository, storeInventoryRepository, shipmentMapper, businessCodeGenerator, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ShipmentService(
            ShipmentRepository shipmentRepository,
            ShipmentItemRepository shipmentItemRepository,
            ShipmentRecommendationRepository recommendationRepository,
            DepotInventoryRepository depotInventoryRepository,
            StoreInventoryRepository storeInventoryRepository,
            ShipmentMapper shipmentMapper,
            BusinessCodeGenerator businessCodeGenerator,
            OperationalActivityService activityService
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.recommendationRepository = recommendationRepository;
        this.depotInventoryRepository = depotInventoryRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.shipmentMapper = shipmentMapper;
        this.businessCodeGenerator = businessCodeGenerator;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipments(
            Long storeId,
            ShipmentStatus status
    ) {
        List<Shipment> shipments;

        if (storeId != null && status != null) {
            shipments = shipmentRepository
                    .findByStoreIdAndStatusOrderByPlannedDispatchDateAsc(
                            storeId,
                            status
                    );
        } else if (storeId != null) {
            shipments = shipmentRepository
                    .findByStoreIdOrderByPlannedDispatchDateDesc(storeId);
        } else if (status != null) {
            shipments = shipmentRepository
                    .findByStatusOrderByPlannedDispatchDateAsc(status);
        } else {
            shipments = shipmentRepository.findAll();
        }

        return shipments.stream().map(shipmentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        return shipmentMapper.toResponse(findShipmentOrThrow(id));
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        validateDates(request);
        List<Long> recommendationIds = request.getRecommendationIds();
        Set<Long> distinctIds = new HashSet<>(recommendationIds);

        if (distinctIds.size() != recommendationIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Recommendation IDs must be unique"
            );
        }

        List<ShipmentRecommendation> recommendations =
                recommendationRepository.findAllById(recommendationIds);

        if (recommendations.size() != recommendationIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more recommendations were not found"
            );
        }

        validateRecommendations(recommendations);

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(businessCodeGenerator.nextShipmentNumber());
        shipment.setStore(recommendations.get(0).getStore());
        shipment.setStatus(ShipmentStatus.PLANNED);
        shipment.setPlannedDispatchDate(request.getPlannedDispatchDate());
        shipment.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        shipment.setNotes(normalizeNotes(request.getNotes()));

        for (ShipmentRecommendation recommendation : recommendations) {
            reserveInventory(recommendation);
            recommendation.setStatus(RecommendationStatus.READY_FOR_TRANSPORT);

            ShipmentItem item = new ShipmentItem();
            item.setProduct(recommendation.getProduct());
            item.setRecommendation(recommendation);
            item.setQuantity(recommendation.getRecommendedShipment());
            shipment.addItem(item);
        }

        recommendationRepository.saveAll(recommendations);
        Shipment saved = shipmentRepository.save(shipment);
        recordActivity(saved, "SHIPMENT_PLANNED", "Shipment planned from approved recommendations");
        return shipmentMapper.toResponse(saved);
    }

    public ShipmentResponse updateStatus(
            Long id,
            UpdateShipmentStatusRequest request
    ) {
        Shipment shipment = findShipmentOrThrow(id);
        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus target = request.getStatus();

        if (current == target) {
            return shipmentMapper.toResponse(shipment);
        }

        validateTransition(current, target);

        switch (target) {
            case READY -> shipment.setStatus(ShipmentStatus.READY);
            case DISPATCHED -> dispatch(shipment);
            case DELIVERED -> deliver(shipment);
            case CANCELLED -> cancel(shipment);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported shipment status transition"
            );
        }

        Shipment saved = shipmentRepository.save(shipment);
        recordActivity(saved, "SHIPMENT_" + target.name(), "Shipment status changed to " + target.name());
        return shipmentMapper.toResponse(saved);
    }

    private void validateDates(CreateShipmentRequest request) {
        if (request.getExpectedDeliveryDate()
                .isBefore(request.getPlannedDispatchDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expected delivery date cannot be before dispatch date"
            );
        }
    }

    private void validateRecommendations(
            List<ShipmentRecommendation> recommendations
    ) {
        Long storeId = recommendations.get(0).getStore().getId();

        for (ShipmentRecommendation recommendation : recommendations) {
            if (!recommendation.getStore().getId().equals(storeId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "All recommendations in a shipment must belong to one store"
                );
            }
            if (recommendation.getStatus() != RecommendationStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only approved recommendations can be planned"
                );
            }
            if (recommendation.getRecommendedShipment() == null
                    || recommendation.getRecommendedShipment() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Shipment quantities must be greater than zero"
                );
            }
            if (shipmentItemRepository.existsByRecommendationId(
                    recommendation.getId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A recommendation is already assigned to a shipment"
                );
            }
        }
    }

    private void reserveInventory(ShipmentRecommendation recommendation) {
        Long productId = recommendation.getProduct().getId();
        int quantity = recommendation.getRecommendedShipment();
        DepotInventory depotInventory = depotInventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Depot inventory is missing for product "
                                + recommendation.getProduct().getProductCode()
                ));
        int freeUnits = depotInventory.getAvailableUnits()
                - depotInventory.getReservedUnits();

        if (freeUnits < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Not enough free depot stock for product "
                            + recommendation.getProduct().getProductCode()
            );
        }

        depotInventory.setReservedUnits(
                depotInventory.getReservedUnits() + quantity
        );
        depotInventory.setLastUpdated(LocalDateTime.now());
        depotInventoryRepository.save(depotInventory);

        StoreInventory storeInventory = storeInventoryRepository
                .findByStoreIdAndProductId(
                        recommendation.getStore().getId(),
                        productId
                )
                .orElseGet(() -> newStoreInventory(recommendation));
        storeInventory.setIncomingUnits(
                storeInventory.getIncomingUnits() + quantity
        );
        storeInventory.setLastUpdated(LocalDateTime.now());
        storeInventoryRepository.save(storeInventory);
    }

    private StoreInventory newStoreInventory(
            ShipmentRecommendation recommendation
    ) {
        StoreInventory inventory = new StoreInventory();
        inventory.setStore(recommendation.getStore());
        inventory.setProduct(recommendation.getProduct());
        inventory.setInventoryLevel(0);
        inventory.setIncomingUnits(0);
        return inventory;
    }

    private void dispatch(Shipment shipment) {
        for (ShipmentItem item : shipment.getItems()) {
            DepotInventory inventory = depotInventoryRepository
                    .findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Depot inventory is missing during dispatch"
                    ));

            if (inventory.getReservedUnits() < item.getQuantity()
                    || inventory.getAvailableUnits() < item.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Reserved depot inventory is inconsistent"
                );
            }

            inventory.setAvailableUnits(
                    inventory.getAvailableUnits() - item.getQuantity()
            );
            inventory.setReservedUnits(
                    inventory.getReservedUnits() - item.getQuantity()
            );
            inventory.setLastUpdated(LocalDateTime.now());
            depotInventoryRepository.save(inventory);
            item.getRecommendation().setStatus(RecommendationStatus.SHIPPED);
        }

        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setDispatchedAt(LocalDateTime.now());
    }

    private void deliver(Shipment shipment) {
        for (ShipmentItem item : shipment.getItems()) {
            StoreInventory inventory = storeInventoryRepository
                    .findByStoreIdAndProductId(
                            shipment.getStore().getId(),
                            item.getProduct().getId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Store inventory is missing during delivery"
                    ));

            inventory.setInventoryLevel(
                    inventory.getInventoryLevel() + item.getQuantity()
            );
            inventory.setIncomingUnits(
                    Math.max(0, inventory.getIncomingUnits() - item.getQuantity())
            );
            inventory.setLastUpdated(LocalDateTime.now());
            storeInventoryRepository.save(inventory);
            item.getRecommendation().setStatus(RecommendationStatus.DELIVERED);
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
    }

    private void cancel(Shipment shipment) {
        for (ShipmentItem item : shipment.getItems()) {
            DepotInventory depotInventory = depotInventoryRepository
                    .findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Depot inventory is missing during cancellation"
                    ));
            depotInventory.setReservedUnits(
                    Math.max(
                            0,
                            depotInventory.getReservedUnits() - item.getQuantity()
                    )
            );
            depotInventory.setLastUpdated(LocalDateTime.now());
            depotInventoryRepository.save(depotInventory);

            storeInventoryRepository.findByStoreIdAndProductId(
                    shipment.getStore().getId(),
                    item.getProduct().getId()
            ).ifPresent(inventory -> {
                inventory.setIncomingUnits(
                        Math.max(
                                0,
                                inventory.getIncomingUnits() - item.getQuantity()
                        )
                );
                inventory.setLastUpdated(LocalDateTime.now());
                storeInventoryRepository.save(inventory);
            });

            item.getRecommendation().setStatus(RecommendationStatus.APPROVED);
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
    }

    private void validateTransition(
            ShipmentStatus current,
            ShipmentStatus target
    ) {
        boolean allowed = switch (current) {
            case PLANNED -> target == ShipmentStatus.READY
                    || target == ShipmentStatus.CANCELLED;
            case READY -> target == ShipmentStatus.DISPATCHED
                    || target == ShipmentStatus.CANCELLED;
            case DISPATCHED -> target == ShipmentStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot move shipment from " + current + " to " + target
            );
        }
    }

    private Shipment findShipmentOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found"
                ));
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    private void recordActivity(Shipment shipment, String type, String detail) {
        if (activityService != null) activityService.record(type, "System", "SHIPMENT", shipment.getId(), shipment.getShipmentNumber(), detail);
    }
}

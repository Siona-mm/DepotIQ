package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.shipment.CreateShipmentRequest;
import com.depotiq.dtos.shipment.ShipmentResponse;
import com.depotiq.dtos.shipment.UpdateShipmentStatusRequest;
import com.depotiq.mappers.ShipmentMapper;
import com.depotiq.models.DepotInventory;
import com.depotiq.models.Product;
import com.depotiq.models.RecommendationStatus;
import com.depotiq.models.Shipment;
import com.depotiq.models.ShipmentItem;
import com.depotiq.models.ShipmentRecommendation;
import com.depotiq.models.ShipmentStatus;
import com.depotiq.models.Store;
import com.depotiq.models.StoreInventory;
import com.depotiq.repositories.DepotInventoryRepository;
import com.depotiq.repositories.ShipmentItemRepository;
import com.depotiq.repositories.ShipmentRecommendationRepository;
import com.depotiq.repositories.ShipmentRepository;
import com.depotiq.repositories.StoreInventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ShipmentServiceTest {
    private ShipmentRepository shipments;
    private ShipmentItemRepository shipmentItems;
    private ShipmentRecommendationRepository recommendations;
    private DepotInventoryRepository depotInventory;
    private StoreInventoryRepository storeInventory;
    private ShipmentMapper mapper;
    private BusinessCodeGenerator businessCodeGenerator;
    private ShipmentService service;

    @BeforeEach
    void setUp() {
        shipments = mock(ShipmentRepository.class);
        shipmentItems = mock(ShipmentItemRepository.class);
        recommendations = mock(ShipmentRecommendationRepository.class);
        depotInventory = mock(DepotInventoryRepository.class);
        storeInventory = mock(StoreInventoryRepository.class);
        mapper = mock(ShipmentMapper.class);
        businessCodeGenerator = mock(BusinessCodeGenerator.class);
        when(businessCodeGenerator.nextShipmentNumber())
                .thenReturn("SHP-2026-0001");
        service = new ShipmentService(
                shipments,
                shipmentItems,
                recommendations,
                depotInventory,
                storeInventory,
                mapper,
                businessCodeGenerator
        );
    }

    @Test
    void createsShipmentAndReservesInventoryForApprovedRecommendation() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation recommendation =
                recommendation(30L, store, product, 40);
        DepotInventory depot = depotInventory(product, 100, 10);
        StoreInventory destination = storeInventory(store, product, 12, 5);
        CreateShipmentRequest request = createRequest(List.of(30L));
        ShipmentResponse response = new ShipmentResponse();

        when(recommendations.findAllByIdForUpdate(List.of(30L)))
                .thenReturn(List.of(recommendation));
        when(shipmentItems.existsByRecommendationId(30L)).thenReturn(false);
        when(depotInventory.findByProductIdForUpdate(20L))
                .thenReturn(Optional.of(depot));
        when(storeInventory.findByStoreIdAndProductIdForUpdate(10L, 20L))
                .thenReturn(Optional.of(destination));
        when(shipments.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        ShipmentResponse result = service.createShipment(request);

        assertThat(result).isSameAs(response);
        assertThat(depot.getReservedUnits()).isEqualTo(50);
        assertThat(depot.getAvailableUnits()).isEqualTo(100);
        assertThat(destination.getIncomingUnits()).isEqualTo(45);
        assertThat(recommendation.getStatus())
                .isEqualTo(RecommendationStatus.READY_FOR_TRANSPORT);
        verify(depotInventory).save(depot);
        verify(storeInventory).save(destination);
        verify(recommendations).saveAll(List.of(recommendation));
        verify(businessCodeGenerator).nextShipmentNumber();
    }

    @Test
    void dispatchesReservedUnitsAndUpdatesRecommendationStatus() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation recommendation =
                recommendation(30L, store, product, 40);
        recommendation.setStatus(RecommendationStatus.READY_FOR_TRANSPORT);
        DepotInventory depot = depotInventory(product, 100, 40);
        Shipment shipment = shipment(
                50L,
                ShipmentStatus.READY,
                store,
                product,
                recommendation,
                40
        );
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest();
        request.setStatus(ShipmentStatus.DISPATCHED);
        ShipmentResponse response = new ShipmentResponse();

        when(shipments.findByIdForUpdate(50L)).thenReturn(Optional.of(shipment));
        when(depotInventory.findByProductIdForUpdate(20L))
                .thenReturn(Optional.of(depot));
        when(shipments.save(shipment)).thenReturn(shipment);
        when(mapper.toResponse(shipment)).thenReturn(response);

        ShipmentResponse result = service.updateStatus(50L, request);

        assertThat(result).isSameAs(response);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DISPATCHED);
        assertThat(shipment.getDispatchedAt()).isNotNull();
        assertThat(depot.getAvailableUnits()).isEqualTo(60);
        assertThat(depot.getReservedUnits()).isZero();
        assertThat(recommendation.getStatus())
                .isEqualTo(RecommendationStatus.SHIPPED);
        verify(depotInventory).save(depot);
    }

    @Test
    void rejectsRecommendationsForDifferentStores() {
        Product product = product(20L, "P0020");
        ShipmentRecommendation first =
                recommendation(30L, store(10L), product, 20);
        ShipmentRecommendation second =
                recommendation(31L, store(11L), product, 25);
        CreateShipmentRequest request = createRequest(List.of(30L, 31L));

        when(recommendations.findAllByIdForUpdate(List.of(30L, 31L)))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.createShipment(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(
                        "All recommendations in a shipment must belong to one store"
                );
    }

    @Test
    void rejectsShipmentWhenFreeDepotStockIsInsufficient() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation recommendation =
                recommendation(30L, store, product, 40);
        DepotInventory depot = depotInventory(product, 50, 20);
        CreateShipmentRequest request = createRequest(List.of(30L));

        when(recommendations.findAllByIdForUpdate(List.of(30L)))
                .thenReturn(List.of(recommendation));
        when(depotInventory.findByProductIdForUpdate(20L))
                .thenReturn(Optional.of(depot));

        assertThatThrownBy(() -> service.createShipment(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not enough free depot stock");
    }

    @Test
    void plansMultipleApprovedRecommendationsForTheSameProduct() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation first =
                recommendation(30L, store, product, 20);
        ShipmentRecommendation second =
                recommendation(31L, store, product, 25);
        DepotInventory depot = depotInventory(product, 100, 10);
        StoreInventory destination = storeInventory(store, product, 12, 5);
        CreateShipmentRequest request = createRequest(List.of(30L, 31L));
        ShipmentResponse response = new ShipmentResponse();

        when(recommendations.findAllByIdForUpdate(List.of(30L, 31L)))
                .thenReturn(List.of(first, second));
        when(depotInventory.findByProductIdForUpdate(20L))
                .thenReturn(Optional.of(depot));
        when(storeInventory.findByStoreIdAndProductIdForUpdate(10L, 20L))
                .thenReturn(Optional.of(destination));
        when(shipments.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        assertThat(service.createShipment(request)).isSameAs(response);
        assertThat(depot.getReservedUnits()).isEqualTo(55);
        assertThat(destination.getIncomingUnits()).isEqualTo(50);
        assertThat(first.getStatus())
                .isEqualTo(RecommendationStatus.READY_FOR_TRANSPORT);
        assertThat(second.getStatus())
                .isEqualTo(RecommendationStatus.READY_FOR_TRANSPORT);
    }

    @Test
    void cancellingPlannedShipmentReleasesReservedAndIncomingUnits() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation recommendation =
                recommendation(30L, store, product, 40);
        recommendation.setStatus(RecommendationStatus.READY_FOR_TRANSPORT);
        DepotInventory depot = depotInventory(product, 100, 40);
        StoreInventory destination = storeInventory(store, product, 12, 45);
        Shipment shipment = shipment(
                50L,
                ShipmentStatus.PLANNED,
                store,
                product,
                recommendation,
                40
        );
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest();
        request.setStatus(ShipmentStatus.CANCELLED);

        when(shipments.findByIdForUpdate(50L)).thenReturn(Optional.of(shipment));
        when(depotInventory.findByProductIdForUpdate(20L))
                .thenReturn(Optional.of(depot));
        when(storeInventory.findByStoreIdAndProductIdForUpdate(10L, 20L))
                .thenReturn(Optional.of(destination));
        when(shipments.save(shipment)).thenReturn(shipment);

        service.updateStatus(50L, request);

        assertThat(depot.getAvailableUnits()).isEqualTo(100);
        assertThat(depot.getReservedUnits()).isZero();
        assertThat(destination.getInventoryLevel()).isEqualTo(12);
        assertThat(destination.getIncomingUnits()).isEqualTo(5);
        assertThat(recommendation.getStatus())
                .isEqualTo(RecommendationStatus.CANCELLED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        verify(recommendations).saveAll(List.of(recommendation));
    }

    @Test
    void deliveringShipmentMovesIncomingUnitsIntoStoreStock() {
        Store store = store(10L);
        Product product = product(20L, "P0020");
        ShipmentRecommendation recommendation =
                recommendation(30L, store, product, 40);
        recommendation.setStatus(RecommendationStatus.SHIPPED);
        StoreInventory destination = storeInventory(store, product, 12, 45);
        Shipment shipment = shipment(
                50L,
                ShipmentStatus.DISPATCHED,
                store,
                product,
                recommendation,
                40
        );
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest();
        request.setStatus(ShipmentStatus.DELIVERED);

        when(shipments.findByIdForUpdate(50L)).thenReturn(Optional.of(shipment));
        when(storeInventory.findByStoreIdAndProductIdForUpdate(10L, 20L))
                .thenReturn(Optional.of(destination));
        when(shipments.save(shipment)).thenReturn(shipment);

        service.updateStatus(50L, request);

        assertThat(destination.getInventoryLevel()).isEqualTo(52);
        assertThat(destination.getIncomingUnits()).isEqualTo(5);
        assertThat(recommendation.getStatus())
                .isEqualTo(RecommendationStatus.DELIVERED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
        verify(storeInventory).save(destination);
        verify(recommendations).saveAll(List.of(recommendation));
    }

    @Test
    void rejectsSkippedShipmentLifecycleSteps() {
        Shipment shipment = new Shipment();
        shipment.setId(50L);
        shipment.setStatus(ShipmentStatus.PLANNED);
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest();
        request.setStatus(ShipmentStatus.DISPATCHED);

        when(shipments.findByIdForUpdate(50L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.updateStatus(50L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot move shipment from PLANNED to DISPATCHED");
    }

    private CreateShipmentRequest createRequest(List<Long> ids) {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setRecommendationIds(ids);
        request.setPlannedDispatchDate(LocalDate.now().plusDays(1));
        request.setExpectedDeliveryDate(LocalDate.now().plusDays(2));
        return request;
    }

    private Store store(Long id) {
        Store store = new Store();
        store.setId(id);
        store.setStoreCode("S" + id);
        store.setName("Store " + id);
        return store;
    }

    private Product product(Long id, String code) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(code);
        product.setName("Product " + id);
        return product;
    }

    private ShipmentRecommendation recommendation(
            Long id,
            Store store,
            Product product,
            int quantity
    ) {
        ShipmentRecommendation recommendation = new ShipmentRecommendation();
        recommendation.setId(id);
        recommendation.setStore(store);
        recommendation.setProduct(product);
        recommendation.setRecommendedShipment(quantity);
        recommendation.setStatus(RecommendationStatus.APPROVED);
        return recommendation;
    }

    private DepotInventory depotInventory(
            Product product,
            int available,
            int reserved
    ) {
        DepotInventory inventory = new DepotInventory();
        inventory.setProduct(product);
        inventory.setAvailableUnits(available);
        inventory.setReservedUnits(reserved);
        return inventory;
    }

    private StoreInventory storeInventory(
            Store store,
            Product product,
            int level,
            int incoming
    ) {
        StoreInventory inventory = new StoreInventory();
        inventory.setStore(store);
        inventory.setProduct(product);
        inventory.setInventoryLevel(level);
        inventory.setIncomingUnits(incoming);
        return inventory;
    }

    private Shipment shipment(
            Long id,
            ShipmentStatus status,
            Store store,
            Product product,
            ShipmentRecommendation recommendation,
            int quantity
    ) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setStore(store);
        shipment.setStatus(status);
        ShipmentItem item = new ShipmentItem();
        item.setProduct(product);
        item.setRecommendation(recommendation);
        item.setQuantity(quantity);
        shipment.addItem(item);
        return shipment;
    }
}

package com.shopstack.service;

import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.dto.warehouse.AllocationResponse;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.Product;
import com.shopstack.entity.Shipment;
import com.shopstack.entity.ShipmentStatus;
import com.shopstack.entity.StockAllocationStatus;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.exception.WarehouseAllocationException;
import com.shopstack.mapper.AllocationMapper;
import com.shopstack.repository.OrderItemWarehouseAllocationRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ShipmentRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseAllocationServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private OrderItemWarehouseAllocationRepository allocationRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private AllocationMapper allocationMapper = new AllocationMapper();

    @InjectMocks
    private WarehouseAllocationServiceImpl warehouseAllocationService;

    private Warehouse warehouse1;
    private Warehouse warehouse2;
    private Product product;
    private WarehouseInventory whInv1;
    private WarehouseInventory whInv2;
    private Order order;
    private OrderItem orderItem;
    private User customer;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(10L)
                .email("customer@shopstack.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .build();

        warehouse1 = Warehouse.builder()
                .id(1L)
                .warehouseCode("WH-BLR-01")
                .name("Bangalore Central")
                .address("100 Hosur Rd")
                .city("Bangalore")
                .state("Karnataka")
                .postalCode("560100")
                .country("India")
                .active(true)
                .build();

        warehouse2 = Warehouse.builder()
                .id(2L)
                .warehouseCode("WH-BOM-01")
                .name("Mumbai Depot")
                .address("200 MIDC")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400093")
                .country("India")
                .active(true)
                .build();

        product = Product.builder()
                .id(50L)
                .name("Noise Cancelling Headphones")
                .sku("SKU-NCH-001")
                .price(BigDecimal.valueOf(4999.00))
                .stockQuantity(100)
                .active(true)
                .build();

        whInv1 = WarehouseInventory.builder()
                .id(101L)
                .warehouse(warehouse1)
                .product(product)
                .totalQuantity(50)
                .reservedQuantity(10)
                .availableQuantity(40)
                .lowStockThreshold(10)
                .build();

        whInv2 = WarehouseInventory.builder()
                .id(102L)
                .warehouse(warehouse2)
                .product(product)
                .totalQuantity(80)
                .reservedQuantity(0)
                .availableQuantity(80)
                .lowStockThreshold(10)
                .build();

        order = Order.builder()
                .id(500L)
                .orderNumber("ORD-TEST12345")
                .user(customer)
                .shippingAddress("123 Main St, Bangalore")
                .items(new ArrayList<>())
                .build();

        orderItem = OrderItem.builder()
                .id(1001L)
                .order(order)
                .product(product)
                .quantity(2)
                .price(BigDecimal.valueOf(4999.00))
                .productNameSnapshot("Noise Cancelling Headphones")
                .productImageUrlSnapshot("https://example.com/headphones.jpg")
                .build();

        order.getItems().add(orderItem);
    }

    @Nested
    @DisplayName("Automatic Warehouse Selection & Allocation Tests")
    class AllocationTests {

        @Test
        @DisplayName("Should select warehouse with highest available stock and reserve inventory")
        void shouldSelectWarehouseWithHighestAvailableStock() {
            // WH2 has 80 available, WH1 has 40 available -> WH2 should be selected
            when(warehouseInventoryRepository.findEligibleWarehousesForProduct(50L, 2))
                    .thenReturn(List.of(whInv2, whInv1));
            when(allocationRepository.save(any(OrderItemWarehouseAllocation.class)))
                    .thenAnswer(inv -> {
                        OrderItemWarehouseAllocation a = inv.getArgument(0);
                        a.setId(777L);
                        return a;
                    });

            List<AllocationResponse> results = warehouseAllocationService.allocateOrderItems(order);

            assertThat(results).hasSize(1);
            AllocationResponse res = results.get(0);
            assertThat(res.getWarehouseCode()).isEqualTo("WH-BOM-01");
            assertThat(res.getAllocatedQuantity()).isEqualTo(2);
            assertThat(res.getAllocationStatus()).isEqualTo("ALLOCATED");

            // Verify stock reservation on WH2
            assertThat(whInv2.getReservedQuantity()).isEqualTo(2);
            assertThat(whInv2.getAvailableQuantity()).isEqualTo(78);
            verify(warehouseInventoryRepository).save(whInv2);
            verify(stockMovementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("Should return existing allocations if already allocated for the order (Idempotency)")
        void shouldReturnExistingAllocationsIfAlreadyAllocated() {
            OrderItemWarehouseAllocation existing = OrderItemWarehouseAllocation.builder()
                    .id(888L)
                    .orderItem(orderItem)
                    .warehouse(warehouse1)
                    .allocatedQuantity(2)
                    .allocationStatus(StockAllocationStatus.ALLOCATED)
                    .build();

            when(allocationRepository.findByOrderId(500L)).thenReturn(List.of(existing));

            List<AllocationResponse> results = warehouseAllocationService.allocateOrderItems(order);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(888L);
            assertThat(results.get(0).getWarehouseCode()).isEqualTo("WH-BLR-01");
        }

        @Test
        @DisplayName("Should handle empty order gracefully")
        void shouldHandleEmptyOrder() {
            Order emptyOrder = Order.builder().id(501L).items(List.of()).build();
            List<AllocationResponse> results = warehouseAllocationService.allocateOrderItems(emptyOrder);
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fulfillment Workflow State Transitions")
    class WorkflowTransitionTests {

        private OrderItemWarehouseAllocation allocation;

        @BeforeEach
        void initAllocation() {
            allocation = OrderItemWarehouseAllocation.builder()
                    .id(999L)
                    .orderItem(orderItem)
                    .warehouse(warehouse1)
                    .warehouseInventory(whInv1)
                    .allocatedQuantity(2)
                    .allocationStatus(StockAllocationStatus.ALLOCATED)
                    .allocatedAt(LocalDateTime.now().minusHours(1))
                    .build();
        }

        @Test
        @DisplayName("Should transition from ALLOCATED to PICKED successfully")
        void shouldTransitionFromAllocatedToPicked() {
            when(allocationRepository.findById(999L)).thenReturn(Optional.of(allocation));
            when(allocationRepository.save(any(OrderItemWarehouseAllocation.class))).thenAnswer(i -> i.getArgument(0));

            AllocationResponse res = warehouseAllocationService.pickAllocation(999L);

            assertThat(res.getAllocationStatus()).isEqualTo("PICKED");
            assertThat(allocation.getPickedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should reject picking if allocation is already PICKED")
        void shouldRejectDuplicatePick() {
            allocation.setAllocationStatus(StockAllocationStatus.PICKED);
            when(allocationRepository.findById(999L)).thenReturn(Optional.of(allocation));

            assertThatThrownBy(() -> warehouseAllocationService.pickAllocation(999L))
                    .isInstanceOf(WarehouseAllocationException.class)
                    .hasMessageContaining("Cannot pick allocation with status: PICKED");
        }

        @Test
        @DisplayName("Should transition from PICKED to PACKED successfully")
        void shouldTransitionFromPickedToPacked() {
            allocation.setAllocationStatus(StockAllocationStatus.PICKED);
            allocation.setPickedAt(LocalDateTime.now().minusMinutes(30));

            when(allocationRepository.findById(999L)).thenReturn(Optional.of(allocation));
            when(allocationRepository.save(any(OrderItemWarehouseAllocation.class))).thenAnswer(i -> i.getArgument(0));

            AllocationActionRequest req = AllocationActionRequest.builder()
                    .packageWeight("1.2 kg")
                    .packageDimensions("20x15x10 cm")
                    .notes("Fragile package")
                    .build();

            AllocationResponse res = warehouseAllocationService.packAllocation(999L, req);

            assertThat(res.getAllocationStatus()).isEqualTo("PACKED");
            assertThat(allocation.getPackedAt()).isNotNull();
            assertThat(allocation.getNotes()).isEqualTo("Fragile package");
        }

        @Test
        @DisplayName("Should reject packing if allocation is not PICKED")
        void shouldRejectPackingBeforePicking() {
            allocation.setAllocationStatus(StockAllocationStatus.ALLOCATED);
            when(allocationRepository.findById(999L)).thenReturn(Optional.of(allocation));

            assertThatThrownBy(() -> warehouseAllocationService.packAllocation(999L, null))
                    .isInstanceOf(WarehouseAllocationException.class)
                    .hasMessageContaining("Cannot pack allocation with status: ALLOCATED");
        }

        @Test
        @DisplayName("Should transition from PACKED to READY_FOR_SHIPMENT and update Shipment")
        void shouldTransitionFromPackedToReadyForShipment() {
            allocation.setAllocationStatus(StockAllocationStatus.PACKED);
            allocation.setPackedAt(LocalDateTime.now().minusMinutes(10));

            Shipment shipment = Shipment.builder()
                    .id(300L)
                    .order(order)
                    .trackingNumber("TRK-TEST-12345")
                    .status(ShipmentStatus.PROCESSING)
                    .build();

            when(allocationRepository.findById(999L)).thenReturn(Optional.of(allocation));
            when(allocationRepository.save(any(OrderItemWarehouseAllocation.class))).thenAnswer(i -> i.getArgument(0));
            when(allocationRepository.findByOrderId(order.getId())).thenReturn(List.of(allocation));
            when(shipmentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(shipment));

            AllocationActionRequest req = AllocationActionRequest.builder()
                    .carrier("BlueDart Express")
                    .build();

            AllocationResponse res = warehouseAllocationService.readyForShipment(999L, req);

            assertThat(res.getAllocationStatus()).isEqualTo("READY_FOR_SHIPMENT");
            assertThat(allocation.getReadyForShipmentAt()).isNotNull();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.READY_TO_SHIP);
            assertThat(shipment.getCarrier()).isEqualTo("BlueDart Express");
        }
    }
}

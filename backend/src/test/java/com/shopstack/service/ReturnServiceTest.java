package com.shopstack.service;

import com.shopstack.dto.returns.QualityCheckRequest;
import com.shopstack.dto.returns.ReturnDecisionRequest;
import com.shopstack.dto.returns.ReturnReceiveRequest;
import com.shopstack.dto.returns.ReturnRequestCreateRequest;
import com.shopstack.dto.returns.ReturnResponse;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Payment;
import com.shopstack.entity.Product;
import com.shopstack.entity.QualityCheckResult;
import com.shopstack.entity.ReturnRequest;
import com.shopstack.entity.ReturnStatus;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.exception.ReturnNotAllowedException;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderItemWarehouseAllocationRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.PaymentRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.ReturnRequestRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReturnServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefundService refundService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderItemWarehouseAllocationRepository allocationRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customer;
    private Warehouse warehouse;
    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("buyer@shopstack.io")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .build();

        warehouse = Warehouse.builder()
                .id(100L)
                .warehouseCode("WH-MUM-01")
                .name("Mumbai West Depot")
                .active(true)
                .build();

        product = Product.builder()
                .id(20L)
                .name("Noise Cancelling Headphones")
                .sku("SKU-AUDIO-001")
                .price(BigDecimal.valueOf(4999.00))
                .stockQuantity(100)
                .build();

        orderItem = OrderItem.builder()
                .id(300L)
                .product(product)
                .productNameSnapshot("Noise Cancelling Headphones")
                .quantity(2)
                .unitPriceSnapshot(BigDecimal.valueOf(4999.00))
                .price(BigDecimal.valueOf(4999.00))
                .build();

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);

        order = Order.builder()
                .id(50L)
                .orderNumber("ORD-12345")
                .user(customer)
                .orderStatus(OrderStatus.DELIVERED)
                .totalAmount(BigDecimal.valueOf(9998.00))
                .items(items)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("buyer@shopstack.io");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should create return request and resolve original fulfilling warehouse")
    void shouldCreateReturnRequestSuccessfully() {
        when(userRepository.findByEmailIgnoreCase("buyer@shopstack.io")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        OrderItemWarehouseAllocation allocation = OrderItemWarehouseAllocation.builder()
                .id(1L)
                .orderItem(orderItem)
                .warehouse(warehouse)
                .allocatedQuantity(2)
                .build();

        when(allocationRepository.findByOrderItemId(300L)).thenReturn(List.of(allocation));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> {
            ReturnRequest req = inv.getArgument(0);
            req.setId(10L);
            return req;
        });

        ReturnRequestCreateRequest req = ReturnRequestCreateRequest.builder()
                .orderItemId(300L)
                .quantity(1)
                .reason(com.shopstack.entity.ReturnReason.DEFECTIVE)
                .description("Right speaker is not functioning")
                .build();

        ReturnResponse response = returnService.createReturnRequest(50L, req);

        assertThat(response).isNotNull();
        assertThat(response.getReturnQuantity()).isEqualTo(1);
        assertThat(response.getOriginalWarehouseCode()).isEqualTo("WH-MUM-01");
        assertThat(response.getRefundAmount()).isEqualByComparingTo("4999.00");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    @DisplayName("Should throw ReturnNotAllowedException when order is not DELIVERED")
    void shouldThrowWhenOrderNotDelivered() {
        order.setOrderStatus(OrderStatus.PROCESSING);
        when(userRepository.findByEmailIgnoreCase("buyer@shopstack.io")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        ReturnRequestCreateRequest req = ReturnRequestCreateRequest.builder()
                .orderItemId(300L)
                .quantity(1)
                .reason(com.shopstack.entity.ReturnReason.DEFECTIVE)
                .build();

        assertThatThrownBy(() -> returnService.createReturnRequest(50L, req))
                .isInstanceOf(ReturnNotAllowedException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    @DisplayName("Should throw InvalidReturnException when return already exists for item")
    void shouldThrowWhenDuplicateReturnRequest() {
        when(userRepository.findByEmailIgnoreCase("buyer@shopstack.io")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        ReturnRequest existing = ReturnRequest.builder()
                .id(99L)
                .order(order)
                .orderItem(orderItem)
                .status(ReturnStatus.REQUESTED)
                .build();

        when(returnRequestRepository.findByOrderId(50L)).thenReturn(Optional.of(existing));

        ReturnRequestCreateRequest req = ReturnRequestCreateRequest.builder()
                .orderItemId(300L)
                .quantity(1)
                .reason(com.shopstack.entity.ReturnReason.DEFECTIVE)
                .build();

        assertThatThrownBy(() -> returnService.createReturnRequest(50L, req))
                .isInstanceOf(com.shopstack.exception.InvalidReturnException.class)
                .hasMessageContaining("already been submitted");
    }

    @Test
    @DisplayName("Should receive return at warehouse and log RETURN_RECEIVED movement")
    void shouldReceiveReturnAtWarehouse() {
        ReturnRequest ret = ReturnRequest.builder()
                .id(10L)
                .returnNumber("RET-ABCD1234")
                .order(order)
                .orderItem(orderItem)
                .user(customer)
                .originalWarehouse(warehouse)
                .returnWarehouse(warehouse)
                .returnQuantity(1)
                .status(ReturnStatus.APPROVED)
                .build();

        when(returnRequestRepository.findById(10L)).thenReturn(Optional.of(ret));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ReturnReceiveRequest receiveReq = ReturnReceiveRequest.builder()
                .packageCondition("Good")
                .receivingNotes("Received parcel at dock bay 3")
                .build();

        ReturnResponse res = returnService.receiveReturnAtWarehouse(10L, receiveReq);

        assertThat(res.getStatus()).isEqualTo(ReturnStatus.ITEM_RECEIVED);
        assertThat(res.getQcStatus()).isEqualTo("QC_PENDING");
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should perform QC ACCEPTED: restock available inventory, log movement, and process refund")
    void shouldPerformQcAcceptedAndRestock() {
        ReturnRequest ret = ReturnRequest.builder()
                .id(10L)
                .returnNumber("RET-ABCD1234")
                .order(order)
                .orderItem(orderItem)
                .user(customer)
                .originalWarehouse(warehouse)
                .returnWarehouse(warehouse)
                .returnQuantity(1)
                .refundAmount(BigDecimal.valueOf(4999.00))
                .status(ReturnStatus.ITEM_RECEIVED)
                .qcStatus("QC_PENDING")
                .build();

        WarehouseInventory wi = WarehouseInventory.builder()
                .id(88L)
                .warehouse(warehouse)
                .product(product)
                .totalQuantity(10)
                .reservedQuantity(0)
                .availableQuantity(10)
                .build();

        when(returnRequestRepository.findById(10L)).thenReturn(Optional.of(ret));
        when(userRepository.findByEmailIgnoreCase("buyer@shopstack.io")).thenReturn(Optional.of(customer));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(100L, 20L)).thenReturn(Optional.of(wi));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(50L)).thenReturn(Optional.of(Payment.builder().id(999L).build()));

        QualityCheckRequest qcReq = QualityCheckRequest.builder()
                .result(QualityCheckResult.ACCEPTED)
                .acceptedQuantity(1)
                .conditionNotes("Condition is pristine, unopened")
                .build();

        ReturnResponse response = returnService.performQualityCheck(10L, qcReq);

        assertThat(response.getQcResult()).isEqualTo(QualityCheckResult.ACCEPTED);
        assertThat(response.getStatus()).isEqualTo(ReturnStatus.REFUNDED);
        assertThat(wi.getTotalQuantity()).isEqualTo(11);
        assertThat(wi.getAvailableQuantity()).isEqualTo(11);
        verify(refundService).createRefundRecord(any(Payment.class), eq(ret), any());
    }

    @Test
    @DisplayName("Should perform QC DAMAGED: isolate to damaged stock without increasing sellable inventory")
    void shouldPerformQcDamagedAndIsolate() {
        ReturnRequest ret = ReturnRequest.builder()
                .id(10L)
                .returnNumber("RET-ABCD1234")
                .order(order)
                .orderItem(orderItem)
                .user(customer)
                .originalWarehouse(warehouse)
                .returnWarehouse(warehouse)
                .returnQuantity(1)
                .refundAmount(BigDecimal.valueOf(4999.00))
                .status(ReturnStatus.ITEM_RECEIVED)
                .qcStatus("QC_PENDING")
                .build();

        WarehouseInventory wi = WarehouseInventory.builder()
                .id(88L)
                .warehouse(warehouse)
                .product(product)
                .totalQuantity(10)
                .reservedQuantity(0)
                .availableQuantity(10)
                .damagedQuantity(0)
                .build();

        when(returnRequestRepository.findById(10L)).thenReturn(Optional.of(ret));
        when(userRepository.findByEmailIgnoreCase("buyer@shopstack.io")).thenReturn(Optional.of(customer));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(100L, 20L)).thenReturn(Optional.of(wi));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        QualityCheckRequest qcReq = QualityCheckRequest.builder()
                .result(QualityCheckResult.DAMAGED)
                .damagedQuantity(1)
                .conditionNotes("Cracked plastic casing")
                .moveToQuarantine(false)
                .build();

        ReturnResponse response = returnService.performQualityCheck(10L, qcReq);

        assertThat(response.getQcResult()).isEqualTo(QualityCheckResult.DAMAGED);
        assertThat(response.getDamagedQuantity()).isEqualTo(1);
        // Sellable quantity MUST NOT increase!
        assertThat(wi.getTotalQuantity()).isEqualTo(10);
        assertThat(wi.getAvailableQuantity()).isEqualTo(10);
        assertThat(wi.getDamagedQuantity()).isEqualTo(1);
    }
}

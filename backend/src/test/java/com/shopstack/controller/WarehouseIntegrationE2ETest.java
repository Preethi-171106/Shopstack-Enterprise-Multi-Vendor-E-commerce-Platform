package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.returns.QualityCheckRequest;
import com.shopstack.dto.returns.ReturnDecisionRequest;
import com.shopstack.dto.returns.ReturnReceiveRequest;
import com.shopstack.dto.returns.ReturnRequestCreateRequest;
import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.dto.warehouse.StockDistributionRequest;
import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseDistributionItem;
import com.shopstack.entity.*;
import com.shopstack.repository.*;
import com.shopstack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class WarehouseIntegrationE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private WarehouseInventoryRepository warehouseInventoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VendorProfileRepository vendorProfileRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemWarehouseAllocationRepository allocationRepository;
    @Autowired private ReturnRequestRepository returnRequestRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User staffUser;
    private User customerUser;
    private User vendorUser;
    private String adminToken;
    private String staffToken;
    private String customerToken;

    private Category category;
    private VendorProfile vendorProfile;
    private Product testProduct;
    private Warehouse initialWarehouse;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = userRepository.save(User.builder()
                .email("admin-" + suffix + "@shopstack.test")
                .password(passwordEncoder.encode("AdminPass123!"))
                .firstName("Admin")
                .lastName("Operator")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(adminUser);

        staffUser = userRepository.save(User.builder()
                .email("staff-" + suffix + "@shopstack.test")
                .password(passwordEncoder.encode("StaffPass123!"))
                .firstName("Warehouse")
                .lastName("Staff")
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build());
        staffToken = jwtService.generateToken(staffUser);

        customerUser = userRepository.save(User.builder()
                .email("cust-" + suffix + "@shopstack.test")
                .password(passwordEncoder.encode("CustPass123!"))
                .firstName("Customer")
                .lastName("Shopper")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());
        customerToken = jwtService.generateToken(customerUser);

        vendorUser = userRepository.save(User.builder()
                .email("vendor-" + suffix + "@shopstack.test")
                .password(passwordEncoder.encode("VendorPass123!"))
                .firstName("Vendor")
                .lastName("Seller")
                .role(UserRole.VENDOR)
                .enabled(true)
                .build());

        vendorProfile = vendorProfileRepository.save(VendorProfile.builder()
                .user(vendorUser)
                .storeName("Tiruppur Textiles Co")
                .businessEmail("contact@tiruppurtextiles.com")
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Apparel " + suffix)
                .slug("apparel-" + suffix)
                .active(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Tiruppur Cotton T-Shirt " + suffix)
                .slug("tshirt-" + suffix)
                .sku("SKU-TSHIRT-" + suffix.toUpperCase())
                .price(new BigDecimal("499.00"))
                .stockQuantity(100)
                .category(category)
                .vendorProfile(vendorProfile)
                .active(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .totalStock(100)
                .reservedStock(0)
                .availableStock(100)
                .lowStockThreshold(10)
                .version(0)
                .build());

        initialWarehouse = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-CHN-01")
                .name("Tiruppur Main Warehouse")
                .address("Industrial Cotton Hub")
                .city("Tiruppur")
                .state("Tamil Nadu")
                .postalCode("641605")
                .country("India")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Complete Warehouse Lifecycle: Create, List, Staff Access, Distribute Stock, Allocation, Pick, Pack, Return, QC, Refund")
    void testCompleteWarehouseWorkflow() throws Exception {
        // ── 1. Create Warehouse via Admin API & Verify Persistence ──
        WarehouseCreateRequest newWhReq = WarehouseCreateRequest.builder()
                .warehouseCode("WH-CBE-01")
                .name("Coimbatore Hub")
                .address("Avinashi Road")
                .city("Coimbatore")
                .state("Tamil Nadu")
                .postalCode("641014")
                .country("India")
                .active(true)
                .build();

        mockMvc.perform(post("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWhReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseCode", is("WH-CBE-01")))
                .andExpect(jsonPath("$.city", is("Coimbatore")));

        // Verify warehouse exists in PostgreSQL repository
        assertThat(warehouseRepository.existsByWarehouseCodeIgnoreCase("WH-CBE-01")).isTrue();
        assertThat(warehouseRepository.existsByWarehouseCodeIgnoreCase("WH-CHN-01")).isTrue();

        // ── 2. Staff & Admin can List All Warehouses (GET /api/admin/warehouses & /api/warehouse/facilities) ──
        mockMvc.perform(get("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/warehouse/facilities")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        // ── 3. Distribute Stock from Central Stock to Tiruppur Warehouse ──
        StockDistributionRequest distReq = StockDistributionRequest.builder()
                .productId(testProduct.getId())
                .distributions(List.of(
                        WarehouseDistributionItem.builder()
                                .warehouseId(initialWarehouse.getId())
                                .quantity(40)
                                .build()
                ))
                .notes("Transfer 40 units to Tiruppur warehouse")
                .build();

        mockMvc.perform(post("/api/admin/warehouses/distribute-stock")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(distReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWarehouseAllocatedStock", is(40)))
                .andExpect(jsonPath("$.unallocatedCentralStock", is(60)));

        // Verify WarehouseInventory in PostgreSQL
        WarehouseInventory wi = warehouseInventoryRepository
                .findByWarehouseIdAndProductId(initialWarehouse.getId(), testProduct.getId())
                .orElseThrow();
        assertThat(wi.getTotalQuantity()).isEqualTo(40);
        assertThat(wi.getAvailableQuantity()).isEqualTo(40);
        assertThat(wi.getReservedQuantity()).isEqualTo(0);

        // ── 4. Place Order as Customer & Verify Multi-Warehouse Allocation ──
        Order order = Order.builder()
                .orderNumber("ORD-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(customerUser)
                .orderStatus(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("998.00"))
                .shippingAddress("123 Main St, Tiruppur, TN 641605")
                .items(new ArrayList<>())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(testProduct)
                .vendorProfile(vendorProfile)
                .quantity(2)
                .price(new BigDecimal("499.00"))
                .productNameSnapshot(testProduct.getName())
                .unitPriceSnapshot(new BigDecimal("499.00"))
                .build();
        order.getItems().add(orderItem);
        Order savedOrder = orderRepository.save(order);
        OrderItem savedItem = savedOrder.getItems().get(0);

        // Create initial allocation
        OrderItemWarehouseAllocation allocation = allocationRepository.save(OrderItemWarehouseAllocation.builder()
                .orderItem(savedItem)
                .warehouse(initialWarehouse)
                .warehouseInventory(wi)
                .allocatedQuantity(2)
                .allocationStatus(StockAllocationStatus.ALLOCATED)
                .build());

        // Reserve stock
        wi.setReservedQuantity(2);
        wi.recalculateAvailableQuantity();
        warehouseInventoryRepository.save(wi);

        assertThat(wi.getAvailableQuantity()).isEqualTo(38);

        // ── 5. Warehouse Staff Pick Item (ALLOCATED -> PICKED) ──
        mockMvc.perform(post("/api/warehouse/allocations/" + allocation.getId() + "/pick")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationStatus", is("PICKED")));

        // ── 6. Warehouse Staff Pack Item (PICKED -> PACKED) ──
        AllocationActionRequest packReq = AllocationActionRequest.builder()
                .packageWeight("0.5 kg")
                .packageDimensions("20x15x5 cm")
                .notes("Packed in standard tamper-evident bag")
                .build();

        mockMvc.perform(post("/api/warehouse/allocations/" + allocation.getId() + "/pack")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationStatus", is("PACKED")));

        // ── 7. Stage for Shipment (PACKED -> READY_FOR_SHIPMENT) ──
        AllocationActionRequest stageReq = AllocationActionRequest.builder()
                .carrier("BlueDart Logistics")
                .notes("Staged in Dispatch Bay 2")
                .build();

        mockMvc.perform(post("/api/warehouse/allocations/" + allocation.getId() + "/ready-for-shipment")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stageReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationStatus", is("READY_FOR_SHIPMENT")));

        // ── 8. Customer Requests Return on Delivered Order ──
        savedOrder.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(savedOrder);

        Payment payment = paymentRepository.save(Payment.builder()
                .order(savedOrder)
                .paymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("998.00"))
                .currency("INR")
                .gateway(PaymentGateway.RAZORPAY)
                .build());

        ReturnRequestCreateRequest retCreateReq = ReturnRequestCreateRequest.builder()
                .orderItemId(savedItem.getId())
                .quantity(1)
                .reason(ReturnReason.QUALITY_ISSUE)
                .description("Product in pristine original condition")
                .build();

        MvcResult retRes = mockMvc.perform(post("/api/returns/orders/" + savedOrder.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retCreateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REQUESTED")))
                .andExpect(jsonPath("$.originalWarehouseCode", is("WH-CHN-01")))
                .andReturn();

        Long returnId = objectMapper.readTree(retRes.getResponse().getContentAsString()).get("id").asLong();

        // ── 9. Admin Approves Return & Verifies Routing to Original Warehouse ──
        ReturnDecisionRequest approveReq = ReturnDecisionRequest.builder()
                .remarks("Return approved by admin")
                .build();

        mockMvc.perform(put("/api/returns/" + returnId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.returnWarehouseCode", is("WH-CHN-01")));

        // ── 10. Staff Receives Return at Warehouse (ITEM_RECEIVED) ──
        ReturnReceiveRequest receiveReq = ReturnReceiveRequest.builder()
                .packageCondition("Outer box intact, seals present")
                .receivingNotes("Parcel received at Intake Dock")
                .build();

        mockMvc.perform(put("/api/returns/" + returnId + "/receive")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ITEM_RECEIVED")))
                .andExpect(jsonPath("$.qcStatus", is("QC_PENDING")));

        // ── 11. Staff Performs Quality Check ACCEPTED -> Restocks & Generates Refund ──
        int stockBeforeQc = warehouseInventoryRepository.findByWarehouseIdAndProductId(initialWarehouse.getId(), testProduct.getId())
                .map(WarehouseInventory::getTotalQuantity).orElse(0);

        QualityCheckRequest qcReq = QualityCheckRequest.builder()
                .result(QualityCheckResult.ACCEPTED)
                .acceptedQuantity(1)
                .damagedQuantity(0)
                .conditionNotes("Brand new, all tags attached. Approved for restock.")
                .moveToQuarantine(false)
                .build();

        mockMvc.perform(put("/api/returns/" + returnId + "/quality-check")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qcReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")))
                .andExpect(jsonPath("$.qcStatus", is("QC_COMPLETED")))
                .andExpect(jsonPath("$.qcResult", is("ACCEPTED")))
                .andExpect(jsonPath("$.acceptedQuantity", is(1)));

        // Verify sellable stock increased by 1
        int stockAfterQc = warehouseInventoryRepository.findByWarehouseIdAndProductId(initialWarehouse.getId(), testProduct.getId())
                .map(WarehouseInventory::getTotalQuantity).orElse(0);
        assertThat(stockAfterQc).isEqualTo(stockBeforeQc + 1);

        // Verify refund record exists in repository
        List<Refund> refunds = refundRepository.findByPayment(payment);
        assertThat(refunds).isNotEmpty();
    }

    @Test
    @DisplayName("Quality Check DAMAGED isolates stock to Damaged/Quarantine and does NOT increase sellable stock")
    void testQualityCheckDamagedWorkflow() throws Exception {
        Order order = Order.builder()
                .orderNumber("ORD-DMG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(customerUser)
                .orderStatus(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("499.00"))
                .shippingAddress("Address")
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(testProduct)
                .vendorProfile(vendorProfile)
                .quantity(1)
                .price(new BigDecimal("499.00"))
                .productNameSnapshot(testProduct.getName())
                .unitPriceSnapshot(new BigDecimal("499.00"))
                .build();
        order.getItems().add(item);
        Order savedOrder = orderRepository.save(order);
        OrderItem savedItem = savedOrder.getItems().get(0);

        ReturnRequest returnReq = returnRequestRepository.save(ReturnRequest.builder()
                .returnNumber("RET-DMG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .order(savedOrder)
                .orderItem(savedItem)
                .returnQuantity(1)
                .user(customerUser)
                .originalWarehouse(initialWarehouse)
                .returnWarehouse(initialWarehouse)
                .status(ReturnStatus.ITEM_RECEIVED)
                .reason(ReturnReason.DAMAGED)
                .qcStatus("QC_PENDING")
                .refundAmount(new BigDecimal("499.00"))
                .requestedAt(java.time.LocalDateTime.now())
                .build());

        WarehouseInventory wiBefore = warehouseInventoryRepository
                .findByWarehouseIdAndProductId(initialWarehouse.getId(), testProduct.getId())
                .orElseGet(() -> warehouseInventoryRepository.save(WarehouseInventory.builder()
                        .warehouse(initialWarehouse)
                        .product(testProduct)
                        .totalQuantity(10)
                        .reservedQuantity(0)
                        .availableQuantity(10)
                        .damagedQuantity(0)
                        .quarantineQuantity(0)
                        .build()));

        int sellableBefore = wiBefore.getTotalQuantity();
        int damagedBefore = wiBefore.getDamagedQuantity();

        QualityCheckRequest qcReq = QualityCheckRequest.builder()
                .result(QualityCheckResult.DAMAGED)
                .acceptedQuantity(0)
                .damagedQuantity(1)
                .conditionNotes("Fabric torn, stained, non-resellable.")
                .moveToQuarantine(false)
                .build();

        mockMvc.perform(put("/api/returns/" + returnReq.getId() + "/quality-check")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qcReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qcResult", is("DAMAGED")))
                .andExpect(jsonPath("$.damagedQuantity", is(1)))
                .andExpect(jsonPath("$.acceptedQuantity", is(0)));

        WarehouseInventory wiAfter = warehouseInventoryRepository
                .findByWarehouseIdAndProductId(initialWarehouse.getId(), testProduct.getId())
                .orElseThrow();

        // Sellable stock MUST NOT increase
        assertThat(wiAfter.getTotalQuantity()).isEqualTo(sellableBefore);
        // Damaged stock MUST increase
        assertThat(wiAfter.getDamagedQuantity()).isEqualTo(damagedBefore + 1);
    }
}

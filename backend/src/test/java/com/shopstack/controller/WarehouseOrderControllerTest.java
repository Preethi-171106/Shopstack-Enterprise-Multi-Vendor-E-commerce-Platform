package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.warehouse.WarehousePackingRequest;
import com.shopstack.dto.warehouse.WarehouseReadyToShipRequest;
import com.shopstack.entity.Category;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.ShipmentRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WarehouseOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorProfileRepository vendorProfileRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String warehouseStaffToken;
    private String adminToken;
    private String customerToken;
    private String vendorToken;

    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create Warehouse Staff
        User warehouseStaff = userRepository.save(User.builder()
                .email("warehouse." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Warehouse")
                .lastName("Staff")
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build());
        warehouseStaffToken = "Bearer " + jwtService.generateToken(warehouseStaff);

        // Create Admin
        User admin = userRepository.save(User.builder()
                .email("admin." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(admin);

        // Create Customer
        User customer = userRepository.save(User.builder()
                .email("customer." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Customer")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());
        customerToken = "Bearer " + jwtService.generateToken(customer);

        // Create Vendor
        User vendorUser = userRepository.save(User.builder()
                .email("vendor." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Vendor")
                .lastName("User")
                .role(UserRole.VENDOR)
                .enabled(true)
                .build());
        vendorToken = "Bearer " + jwtService.generateToken(vendorUser);

        VendorProfile vendorProfile = vendorProfileRepository.save(VendorProfile.builder()
                .user(vendorUser)
                .storeName("Apex Logistics Hub")
                .businessEmail("apex@logistics.com")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Industrial Equipment " + UUID.randomUUID())
                .slug("industrial-equipment-" + UUID.randomUUID())
                .description("Warehouse tools and components")
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Warehouse Barcode Scanner")
                .slug("scanner-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase())
                .sku("SCANNER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .category(category)
                .vendorProfile(vendorProfile)
                .price(BigDecimal.valueOf(2499.00))
                .stockQuantity(50)
                .active(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .totalStock(50)
                .reservedStock(2)
                .availableStock(48)
                .lowStockThreshold(10)
                .build());

        testOrder = Order.builder()
                .user(customer)
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .shippingAddress("123 Industrial Way, Sector 4, Bangalore")
                .orderStatus(OrderStatus.PROCESSING)
                .subtotalAmount(BigDecimal.valueOf(4998.00))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(4998.00))
                .build();

        OrderItem item = OrderItem.builder()
                .order(testOrder)
                .product(testProduct)
                .vendorProfile(vendorProfile)
                .quantity(2) // Exact quantity
                .price(BigDecimal.valueOf(2499.00))
                .productNameSnapshot(testProduct.getName())
                .productImageUrlSnapshot("https://example.com/scanner.jpg")
                .unitPriceSnapshot(BigDecimal.valueOf(2499.00))
                .build();
        testOrder.getItems().add(item);

        testOrder = orderRepository.save(testOrder);
    }

    @Nested
    @DisplayName("Warehouse Dashboard & Order Queue Tests")
    class DashboardAndQueueTests {

        @Test
        @DisplayName("GET /api/warehouse/dashboard returns 200 with aggregated metrics")
        void getWarehouseDashboard_asWarehouseStaff_returns200() throws Exception {
            mockMvc.perform(get("/api/warehouse/dashboard")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcessingOrders", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.totalTrackedInventories", greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("GET /api/warehouse/orders returns 200 and preserves exact quantity")
        void getWarehouseOrders_exactQuantityPreserved() throws Exception {
            mockMvc.perform(get("/api/warehouse/orders")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].items[0].quantity", is(2))); // Quantity 2 preserved
        }

        @Test
        @DisplayName("GET /api/warehouse/orders/{orderId} returns 200 with inventory and SKU info")
        void getWarehouseOrderById_returnsDetails() throws Exception {
            mockMvc.perform(get("/api/warehouse/orders/" + testOrder.getId())
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId", is(testOrder.getId().intValue())))
                    .andExpect(jsonPath("$.items[0].productSku", is(testProduct.getSku())))
                    .andExpect(jsonPath("$.items[0].availableStock", is(48)));
        }
    }

    @Nested
    @DisplayName("Warehouse Order Lifecycle Workflow Tests")
    class OrderWorkflowTests {

        @Test
        @DisplayName("Full Workflow: start-picking -> mark-picked -> start-packing -> mark-packed -> ready-to-ship")
        void testFullWarehouseWorkflow() throws Exception {
            // 1. Start Picking
            mockMvc.perform(post("/api/warehouse/orders/" + testOrder.getId() + "/start-picking")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseStatus", is("PICKING")));

            // 2. Mark Picked
            mockMvc.perform(post("/api/warehouse/orders/" + testOrder.getId() + "/mark-picked")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseStatus", is("PICKED")));

            // 3. Start Packing
            mockMvc.perform(post("/api/warehouse/orders/" + testOrder.getId() + "/start-packing")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseStatus", is("PACKING")));

            // 4. Mark Packed
            WarehousePackingRequest packReq = WarehousePackingRequest.builder()
                    .packageWeight("1.2 kg")
                    .packageDimensions("20x15x10 cm")
                    .packingNotes("Double bubble wrapped")
                    .build();

            mockMvc.perform(post("/api/warehouse/orders/" + testOrder.getId() + "/mark-packed")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(packReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseStatus", is("PACKED")));

            // 5. Ready to Ship
            WarehouseReadyToShipRequest shipReq = WarehouseReadyToShipRequest.builder()
                    .carrier("BlueDart Air Express")
                    .shippingNotes("Staged in Dispatch Bay A-3")
                    .build();

            mockMvc.perform(post("/api/warehouse/orders/" + testOrder.getId() + "/ready-to-ship")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shipReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseStatus", is("READY_TO_SHIP")))
                    .andExpect(jsonPath("$.carrier", is("BlueDart Air Express")));
        }
    }

    @Nested
    @DisplayName("Security & Role Authorization Tests")
    class SecurityAuthorizationTests {

        @Test
        @DisplayName("Unauthenticated request to /api/warehouse/dashboard returns 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/warehouse/dashboard"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Customer request to /api/warehouse/dashboard returns 403 Forbidden")
        void customer_returns403() throws Exception {
            mockMvc.perform(get("/api/warehouse/dashboard")
                            .header("Authorization", customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Vendor request to /api/warehouse/dashboard returns 403 Forbidden")
        void vendor_returns403() throws Exception {
            mockMvc.perform(get("/api/warehouse/dashboard")
                            .header("Authorization", vendorToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin request to /api/warehouse/dashboard returns 200 OK")
        void admin_returns200() throws Exception {
            mockMvc.perform(get("/api/warehouse/dashboard")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }
}

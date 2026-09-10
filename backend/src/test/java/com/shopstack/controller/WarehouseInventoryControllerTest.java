package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.inventory.InventoryThresholdUpdateRequest;
import com.shopstack.dto.inventory.StockAddRequest;
import com.shopstack.dto.inventory.StockAdjustmentRequest;
import com.shopstack.entity.Category;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.StockMovementRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WarehouseInventoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorProfileRepository vendorProfileRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String warehouseStaffToken;
    private String adminToken;
    private String customerToken;
    private String vendorToken;

    private Product product1;
    private Product product2;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        // Warehouse Staff
        User staff = userRepository.save(User.builder()
                .email("staff." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Warehouse")
                .lastName("Specialist")
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build());
        warehouseStaffToken = "Bearer " + jwtService.generateToken(staff);

        // Admin
        User admin = userRepository.save(User.builder()
                .email("admin." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Admin")
                .lastName("Super")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(admin);

        // Customer
        User customer = userRepository.save(User.builder()
                .email("cust." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Customer")
                .lastName("Buyer")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());
        customerToken = "Bearer " + jwtService.generateToken(customer);

        // Vendor
        User vendorUser = userRepository.save(User.builder()
                .email("vendor." + UUID.randomUUID() + "@shopstack.com")
                .password(passwordEncoder.encode("Pass@12345"))
                .firstName("Vendor")
                .lastName("Seller")
                .role(UserRole.VENDOR)
                .enabled(true)
                .build());
        vendorToken = "Bearer " + jwtService.generateToken(vendorUser);

        VendorProfile vendorProfile = vendorProfileRepository.save(VendorProfile.builder()
                .user(vendorUser)
                .storeName("Global Electronics Direct")
                .businessEmail("global@electronics.com")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Logistics Equipment " + UUID.randomUUID())
                .slug("logistics-eq-" + UUID.randomUUID())
                .description("Warehouse sensors and labels")
                .build());

        product1 = productRepository.save(Product.builder()
                .name("High-Speed Thermal Label Printer")
                .slug("label-printer-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase())
                .sku("PRINTER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .category(category)
                .vendorProfile(vendorProfile)
                .price(BigDecimal.valueOf(12999.00))
                .stockQuantity(100)
                .active(true)
                .build());

        inventory1 = inventoryRepository.save(Inventory.builder()
                .product(product1)
                .totalStock(100)
                .reservedStock(5)
                .availableStock(95)
                .lowStockThreshold(15)
                .build());

        product2 = productRepository.save(Product.builder()
                .name("RFID Pallet Tags (Pack of 500)")
                .slug("rfid-tags-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase())
                .sku("RFID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .category(category)
                .vendorProfile(vendorProfile)
                .price(BigDecimal.valueOf(4500.00))
                .stockQuantity(8)
                .active(true)
                .build());

        inventory2 = inventoryRepository.save(Inventory.builder()
                .product(product2)
                .totalStock(8)
                .reservedStock(0)
                .availableStock(8)
                .lowStockThreshold(10) // Low stock!
                .build());
    }

    @Nested
    @DisplayName("Inventory Listing & Filtering Tests")
    class InventoryListingTests {

        @Test
        @DisplayName("GET /api/warehouse/inventory returns all inventories for staff")
        void getAllInventories_returnsList() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("GET /api/warehouse/inventory with search query returns matching SKU")
        void getInventories_withSearch_returnsMatched() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory")
                            .param("search", product1.getSku())
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].productSku", is(product1.getSku())));
        }

        @Test
        @DisplayName("GET /api/warehouse/inventory/low-stock returns low stock items")
        void getLowStockInventories_returnsLowStock() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory/low-stock")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("GET /api/warehouse/inventory/{productId} returns inventory details")
        void getInventoryByProductId_returnsDetails() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory/" + product1.getId())
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId", is(product1.getId().intValue())))
                    .andExpect(jsonPath("$.totalStock", is(100)))
                    .andExpect(jsonPath("$.availableStock", is(95)));
        }
    }

    @Nested
    @DisplayName("Stock Adjustment & Audit Trail Tests")
    class StockAdjustmentTests {

        @Test
        @DisplayName("POST /api/warehouse/inventory/{productId}/add adds units and logs movement")
        void addStock_success() throws Exception {
            StockAddRequest req = new StockAddRequest(50, "PO-9001", "Supplier shipment receipt");

            mockMvc.perform(post("/api/warehouse/inventory/" + product1.getId() + "/add")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStock", is(150)))
                    .andExpect(jsonPath("$.availableStock", is(145)));
        }

        @Test
        @DisplayName("POST /api/warehouse/inventory/{productId}/adjust with direct override works")
        void adjustStock_directOverride_success() throws Exception {
            StockAdjustmentRequest req = StockAdjustmentRequest.builder()
                    .newTotalStock(120)
                    .reason("Physical stocktake audit")
                    .build();

            mockMvc.perform(post("/api/warehouse/inventory/" + product1.getId() + "/adjust")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStock", is(120)))
                    .andExpect(jsonPath("$.availableStock", is(115)));
        }

        @Test
        @DisplayName("POST /api/warehouse/inventory/{productId}/adjust with DAMAGE delta reduces stock")
        void adjustStock_damageOperation_success() throws Exception {
            StockAdjustmentRequest req = StockAdjustmentRequest.builder()
                    .adjustmentType("DAMAGE")
                    .quantity(10)
                    .reason("Water damage during warehouse roof leak")
                    .build();

            mockMvc.perform(post("/api/warehouse/inventory/" + product1.getId() + "/adjust")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStock", is(90)))
                    .andExpect(jsonPath("$.availableStock", is(85)));
        }

        @Test
        @DisplayName("POST /api/warehouse/inventory/{productId}/adjust below reserved stock throws 400")
        void adjustStock_belowReserved_fails() throws Exception {
            StockAdjustmentRequest req = StockAdjustmentRequest.builder()
                    .newTotalStock(2) // Reserved is 5!
                    .reason("Invalid adjustment")
                    .build();

            mockMvc.perform(post("/api/warehouse/inventory/" + product1.getId() + "/adjust")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT /api/warehouse/inventory/{productId}/threshold updates low-stock alert threshold")
        void updateThreshold_success() throws Exception {
            InventoryThresholdUpdateRequest req = new InventoryThresholdUpdateRequest(25);

            mockMvc.perform(put("/api/warehouse/inventory/" + product1.getId() + "/threshold")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lowStockThreshold", is(25)));
        }

        @Test
        @DisplayName("GET /api/warehouse/stock-movements returns paginated movements")
        void getStockMovements_returnsMovements() throws Exception {
            // First perform an addition to ensure at least 1 movement exists
            StockAddRequest req = new StockAddRequest(20, "PO-100", "Inbound");
            mockMvc.perform(post("/api/warehouse/inventory/" + product1.getId() + "/add")
                            .header("Authorization", warehouseStaffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/warehouse/stock-movements")
                            .header("Authorization", warehouseStaffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("RBAC Security Boundaries")
    class RBACSecurityTests {

        @Test
        @DisplayName("Unauthenticated access to /api/warehouse/inventory returns 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Customer access to /api/warehouse/inventory returns 403")
        void customer_returns403() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory")
                            .header("Authorization", customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Vendor access to /api/warehouse/inventory returns 403")
        void vendor_returns403() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory")
                            .header("Authorization", vendorToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin access to /api/warehouse/inventory returns 200 OK")
        void admin_returns200() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }
}

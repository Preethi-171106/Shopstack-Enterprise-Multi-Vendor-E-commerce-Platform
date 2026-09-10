package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.entity.Category;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Product;
import com.shopstack.entity.StockAllocationStatus;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.OrderItemWarehouseAllocationRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WarehouseAllocationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorProfileRepository vendorProfileRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private WarehouseInventoryRepository warehouseInventoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemWarehouseAllocationRepository allocationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String staffToken;
    private String adminToken;
    private String customerToken;
    private OrderItemWarehouseAllocation testAllocation;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User staff = userRepository.save(User.builder()
                .email("staff-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("Operator")
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build());
        staffToken = jwtService.generateToken(staff);

        User admin = userRepository.save(User.builder()
                .email("admin-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Manager")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(admin);

        User customer = userRepository.save(User.builder()
                .email("customer-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Customer")
                .lastName("Buyer")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());
        customerToken = jwtService.generateToken(customer);

        VendorProfile vendor = vendorProfileRepository.save(VendorProfile.builder()
                .user(admin)
                .storeName("Official Tech Store " + suffix)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Electronics " + suffix)
                .slug("electronics-" + suffix)
                .active(true)
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Mechanical Keyboard " + suffix)
                .slug("mechanical-keyboard-" + suffix)
                .sku("SKU-KB-" + suffix)
                .price(BigDecimal.valueOf(3499.00))
                .stockQuantity(100)
                .category(category)
                .vendorProfile(vendor)
                .active(true)
                .build());

        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-PUN-" + suffix.toUpperCase())
                .name("Pune Mega Hub " + suffix)
                .address("Hinjewadi Phase 1")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411057")
                .country("India")
                .active(true)
                .build());

        WarehouseInventory whInv = warehouseInventoryRepository.save(WarehouseInventory.builder()
                .warehouse(warehouse)
                .product(product)
                .totalQuantity(50)
                .reservedQuantity(2)
                .availableQuantity(48)
                .lowStockThreshold(10)
                .build());

        Order order = Order.builder()
                .user(customer)
                .orderNumber("ORD-" + suffix.toUpperCase())
                .shippingAddress("456 Park Avenue, Pune")
                .orderStatus(OrderStatus.PROCESSING)
                .totalAmount(BigDecimal.valueOf(6998.00))
                .items(new ArrayList<>())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .vendorProfile(vendor)
                .quantity(2)
                .price(BigDecimal.valueOf(3499.00))
                .productNameSnapshot(product.getName())
                .build();
        order.getItems().add(orderItem);

        order = orderRepository.save(order);
        OrderItem savedItem = order.getItems().get(0);

        testAllocation = allocationRepository.save(OrderItemWarehouseAllocation.builder()
                .orderItem(savedItem)
                .warehouse(warehouse)
                .warehouseInventory(whInv)
                .allocatedQuantity(2)
                .allocationStatus(StockAllocationStatus.ALLOCATED)
                .allocatedAt(LocalDateTime.now())
                .notes("Allocated for order " + order.getOrderNumber())
                .build());
    }

    @Test
    @DisplayName("Warehouse staff can list allocations - 200 OK")
    void staffCanListAllocations() throws Exception {
        mockMvc.perform(get("/api/warehouse/allocations")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Staff can pick an allocated item - 200 OK")
    void staffCanPickAllocation() throws Exception {
        mockMvc.perform(post("/api/warehouse/allocations/" + testAllocation.getId() + "/pick")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationStatus", is("PICKED")));
    }

    @Test
    @DisplayName("Staff can pack a picked item - 200 OK")
    void staffCanPackAllocation() throws Exception {
        // First pick
        testAllocation.setAllocationStatus(StockAllocationStatus.PICKED);
        testAllocation.setPickedAt(LocalDateTime.now());
        allocationRepository.save(testAllocation);

        AllocationActionRequest req = AllocationActionRequest.builder()
                .packageWeight("1.5 kg")
                .packageDimensions("30x20x10 cm")
                .notes("Standard protective packing")
                .build();

        mockMvc.perform(post("/api/warehouse/allocations/" + testAllocation.getId() + "/pack")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationStatus", is("PACKED")));
    }

    @Test
    @DisplayName("Customer cannot access warehouse allocations - 403 Forbidden")
    void customerCannotAccessAllocations() throws Exception {
        mockMvc.perform(get("/api/warehouse/allocations")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}

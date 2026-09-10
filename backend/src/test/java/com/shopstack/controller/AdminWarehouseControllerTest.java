package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.Warehouse;
import com.shopstack.repository.UserRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminWarehouseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String staffToken;
    private String customerToken;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User admin = userRepository.save(User.builder()
                .email("admin-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(admin);

        User staff = userRepository.save(User.builder()
                .email("staff-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("User")
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build());
        staffToken = jwtService.generateToken(staff);

        User customer = userRepository.save(User.builder()
                .email("customer-" + suffix + "@shopstack.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Customer")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());
        customerToken = jwtService.generateToken(customer);

        testWarehouse = warehouseRepository.save(Warehouse.builder()
                .warehouseCode("WH-TEST-" + suffix.toUpperCase())
                .name("Test Fulfillment Center")
                .address("123 Test Street")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Admin can list all warehouses - 200 OK")
    void adminCanListWarehouses() throws Exception {
        mockMvc.perform(get("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Admin can create new warehouse - 201 Created")
    void adminCanCreateWarehouse() throws Exception {
        WarehouseCreateRequest req = WarehouseCreateRequest.builder()
                .warehouseCode("WH-DEL-01")
                .name("Delhi North Fulfillment Hub")
                .address("Sector 18, Rohini")
                .city("New Delhi")
                .state("Delhi")
                .postalCode("110085")
                .country("India")
                .active(true)
                .build();

        mockMvc.perform(post("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseCode", is("WH-DEL-01")))
                .andExpect(jsonPath("$.city", is("New Delhi")));
    }

    @Test
    @DisplayName("Customer cannot access admin warehouse endpoints - 403 Forbidden")
    void customerCannotAccessAdminWarehouses() throws Exception {
        mockMvc.perform(get("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/warehouses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can update warehouse - 200 OK")
    void adminCanUpdateWarehouse() throws Exception {
        WarehouseUpdateRequest req = WarehouseUpdateRequest.builder()
                .name("Updated Test Center Name")
                .address("456 Updated Lane")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .active(true)
                .build();

        mockMvc.perform(put("/api/admin/warehouses/" + testWarehouse.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Test Center Name")));
    }

    @Test
    @DisplayName("Admin can deactivate warehouse - 200 OK")
    void adminCanDeactivateWarehouse() throws Exception {
        mockMvc.perform(patch("/api/admin/warehouses/" + testWarehouse.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    @DisplayName("Warehouse staff can list all warehouses - 200 OK")
    void warehouseStaffCanListWarehouses() throws Exception {
        mockMvc.perform(get("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Warehouse staff can view warehouse by ID - 200 OK")
    void warehouseStaffCanGetWarehouseById() throws Exception {
        mockMvc.perform(get("/api/admin/warehouses/" + testWarehouse.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseCode", is(testWarehouse.getWarehouseCode())));
    }

    @Test
    @DisplayName("Warehouse staff cannot create warehouse - 403 Forbidden")
    void warehouseStaffCannotCreateWarehouse() throws Exception {
        WarehouseCreateRequest req = WarehouseCreateRequest.builder()
                .warehouseCode("WH-STAFF-FORBIDDEN")
                .name("Staff Created Warehouse")
                .address("Some Address")
                .city("City")
                .state("State")
                .postalCode("123456")
                .country("India")
                .active(true)
                .build();

        mockMvc.perform(post("/api/admin/warehouses")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}

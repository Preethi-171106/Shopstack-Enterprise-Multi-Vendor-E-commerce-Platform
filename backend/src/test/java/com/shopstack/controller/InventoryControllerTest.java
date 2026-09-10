package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.inventory.InventoryCreateRequest;
import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.dto.inventory.InventoryThresholdUpdateRequest;
import com.shopstack.dto.inventory.StockAddRequest;
import com.shopstack.dto.inventory.StockAdjustmentRequest;
import com.shopstack.dto.inventory.StockMovementResponse;
import com.shopstack.dto.inventory.StockReleaseRequest;
import com.shopstack.dto.inventory.StockReserveRequest;
import com.shopstack.dto.inventory.StockReturnRequest;
import com.shopstack.entity.StockMovementType;
import com.shopstack.exception.DuplicateInventoryException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.InventoryNotFoundException;
import com.shopstack.exception.InvalidStockOperationException;
import com.shopstack.exception.ProductOwnershipException;
import com.shopstack.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private InventoryResponse sampleInventoryResponse;
    private StockMovementResponse sampleMovementResponse;

    @BeforeEach
    void setUp() {
        sampleInventoryResponse = new InventoryResponse(
                1L, 100L, "Sample Product", "SKU-100", 10L, "Sample Vendor",
                50, 10, 40, 15, false, 0,
                LocalDateTime.now(), LocalDateTime.now()
        );

        sampleMovementResponse = new StockMovementResponse(
                1L, 100L, "Sample Product", "SKU-100", StockMovementType.IN,
                50, 0, 50, "REF-001", "Initial stock", 1L, "warehouse@shopstack.com",
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Warehouse Inventory API Tests (/api/warehouse/inventory)")
    class WarehouseInventoryApiTests {

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("WAREHOUSE_STAFF can create inventory — 201 Created")
        void createInventory_WarehouseStaff_Success() throws Exception {
            InventoryCreateRequest request = new InventoryCreateRequest(100L, 50, 15);
            given(inventoryService.createInventory(any(InventoryCreateRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(100L))
                    .andExpect(jsonPath("$.totalStock").value(50))
                    .andExpect(jsonPath("$.availableStock").value(40));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Stock IN: WAREHOUSE_STAFF can add stock — 200 OK")
        void addStock_WarehouseStaff_Success() throws Exception {
            StockAddRequest request = new StockAddRequest(20, "PO-123", "Restock");
            given(inventoryService.addStock(eq(100L), any(StockAddRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory/product/100/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(100L));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Stock RESERVE: WAREHOUSE_STAFF can reserve stock — 200 OK")
        void reserveStock_WarehouseStaff_Success() throws Exception {
            StockReserveRequest request = new StockReserveRequest(5, "ORD-99", "Order placed");
            given(inventoryService.reserveStock(eq(100L), any(StockReserveRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory/product/100/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Stock RELEASE: WAREHOUSE_STAFF can release reserved stock — 200 OK")
        void releaseStock_WarehouseStaff_Success() throws Exception {
            StockReleaseRequest request = new StockReleaseRequest(5, "ORD-99", "Order cancelled");
            given(inventoryService.releaseReservedStock(eq(100L), any(StockReleaseRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory/product/100/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Stock RETURN: WAREHOUSE_STAFF can record stock return — 200 OK")
        void returnStock_WarehouseStaff_Success() throws Exception {
            StockReturnRequest request = new StockReturnRequest(2, "RMA-01", "Customer return");
            given(inventoryService.recordReturnStock(eq(100L), any(StockReturnRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory/product/100/return")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Stock ADJUSTMENT: WAREHOUSE_STAFF can adjust total stock — 200 OK")
        void adjustStock_WarehouseStaff_Success() throws Exception {
            StockAdjustmentRequest request = new StockAdjustmentRequest(60, "AUD-1", "Stocktake");
            given(inventoryService.adjustStock(eq(100L), any(StockAdjustmentRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/warehouse/inventory/product/100/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Update Low Stock Threshold — 200 OK")
        void updateThreshold_Success() throws Exception {
            InventoryThresholdUpdateRequest request = new InventoryThresholdUpdateRequest(25);
            given(inventoryService.updateLowStockThreshold(eq(100L), any(InventoryThresholdUpdateRequest.class))).willReturn(sampleInventoryResponse);

            mockMvc.perform(put("/api/warehouse/inventory/product/100/threshold")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("List Low Stock Products — 200 OK")
        void getLowStock_Success() throws Exception {
            given(inventoryService.getLowStockInventories()).willReturn(List.of(sampleInventoryResponse));

            mockMvc.perform(get("/api/warehouse/inventory/low-stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(100L));
        }
    }

    @Nested
    @DisplayName("Admin Inventory API Tests (/api/admin/inventory)")
    class AdminInventoryApiTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can list all inventories — 200 OK")
        void getAllInventories_Admin_Success() throws Exception {
            given(inventoryService.getAllInventories(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleInventoryResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/inventory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(100L));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can view all stock movement history — 200 OK")
        void getAllMovements_Admin_Success() throws Exception {
            given(inventoryService.getAllStockMovements(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleMovementResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/inventory/movements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].movementType").value("IN"));
        }
    }

    @Nested
    @DisplayName("Vendor Inventory API Tests (/api/vendor/inventory)")
    class VendorInventoryApiTests {

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("VENDOR can view their own inventory list — 200 OK")
        void getVendorInventories_Vendor_Success() throws Exception {
            given(inventoryService.getVendorInventories(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleInventoryResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/vendor/inventory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(100L));
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("VENDOR accessing another vendor's product inventory returns 403 Forbidden")
        void getInventory_OtherVendorProduct_Forbidden() throws Exception {
            given(inventoryService.getInventoryByProductId(999L))
                    .willThrow(new ProductOwnershipException("Access denied. Product belongs to another vendor"));

            mockMvc.perform(get("/api/vendor/inventory/product/999"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied. Product belongs to another vendor"));
        }
    }

    @Nested
    @DisplayName("Security & Role-Based Access Control (RBAC) Tests")
    class RbacAndSecurityTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER role accessing warehouse inventory APIs returns 403 Forbidden")
        void customerRole_AccessWarehouseInventory_Forbidden() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER role accessing admin inventory APIs returns 403 Forbidden")
        void customerRole_AccessAdminInventory_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/inventory"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER role accessing vendor inventory APIs returns 403 Forbidden")
        void customerRole_AccessVendorInventory_Forbidden() throws Exception {
            mockMvc.perform(get("/api/vendor/inventory"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedAccess_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/warehouse/inventory"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Error Handling & Validation Tests")
    class ExceptionAndValidationTests {

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Negative quantity input returns 400 Bad Request (Bean Validation)")
        void addStock_NegativeQuantity_BadRequest() throws Exception {
            StockAddRequest request = new StockAddRequest(-5, "PO-1", "Invalid");

            mockMvc.perform(post("/api/warehouse/inventory/product/100/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Insufficient stock reservation returns 400 Bad Request")
        void reserveStock_InsufficientStock_BadRequest() throws Exception {
            StockReserveRequest request = new StockReserveRequest(100, "ORD-99", "Exceeds");
            given(inventoryService.reserveStock(eq(100L), any(StockReserveRequest.class)))
                    .willThrow(new InsufficientStockException("Insufficient available stock"));

            mockMvc.perform(post("/api/warehouse/inventory/product/100/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Insufficient available stock"));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Duplicate inventory creation returns 409 Conflict")
        void createInventory_Duplicate_Conflict() throws Exception {
            InventoryCreateRequest request = new InventoryCreateRequest(100L, 50, 10);
            given(inventoryService.createInventory(any(InventoryCreateRequest.class)))
                    .willThrow(new DuplicateInventoryException("Inventory record already exists for product ID: 100"));

            mockMvc.perform(post("/api/warehouse/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Inventory record already exists for product ID: 100"));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Inventory lookup for missing product returns 404 Not Found")
        void getInventory_NotFound() throws Exception {
            given(inventoryService.getInventoryByProductId(999L))
                    .willThrow(new InventoryNotFoundException("Inventory record not found for product ID: 999"));

            mockMvc.perform(get("/api/warehouse/inventory/product/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Inventory record not found for product ID: 999"));
        }
    }
}

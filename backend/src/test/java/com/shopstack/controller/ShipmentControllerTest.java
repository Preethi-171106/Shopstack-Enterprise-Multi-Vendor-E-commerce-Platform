package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.shipment.ShipmentCreateRequest;
import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.dto.shipment.ShipmentStatusUpdateRequest;
import com.shopstack.dto.shipment.TrackingEventResponse;
import com.shopstack.entity.ShipmentStatus;
import com.shopstack.exception.InvalidShipmentStatusTransitionException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.ShipmentAlreadyExistsException;
import com.shopstack.exception.ShipmentNotFoundException;
import com.shopstack.exception.ShipmentOwnershipException;
import com.shopstack.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShipmentControllerTest — MockMvc integration tests for all Shipment Management APIs.
 *
 * <p>Covers:
 * <ul>
 *   <li>Warehouse Staff / Admin: create, list, get, update status</li>
 *   <li>Duplicate shipment prevention (409 Conflict)</li>
 *   <li>Valid and invalid status transitions</li>
 *   <li>Customer ownership checks (own orders only)</li>
 *   <li>Vendor shipment visibility (own products only)</li>
 *   <li>Tracking number lookup</li>
 *   <li>Unauthenticated (401) and forbidden (403) access</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    private ShipmentResponse sampleShipmentResponse() {
        TrackingEventResponse event = new TrackingEventResponse(
                1L,
                ShipmentStatus.PROCESSING,
                "Warehouse - Chennai",
                "Shipment created and is being processed",
                LocalDateTime.now()
        );
        return new ShipmentResponse(
                1L,
                "TRK-20260727-AB123",
                10L,
                "ORD-2026-0001",
                "FedEx",
                ShipmentStatus.PROCESSING,
                "123 Main St, Chennai, India",
                LocalDateTime.now().plusDays(5),
                null,
                null,
                List.of(event),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ShipmentCreateRequest validCreateRequest() {
        return new ShipmentCreateRequest(
                10L,
                "FedEx",
                "123 Main St, Chennai, India",
                LocalDateTime.now().plusDays(5),
                "Warehouse - Chennai",
                "Shipment created and is being processed"
        );
    }

    // =========================================================================
    // Warehouse — Create Shipment
    // =========================================================================

    @Nested
    @DisplayName("POST /api/warehouse/shipments — Create Shipment")
    class WarehouseCreateShipment {

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — creates shipment (201 Created)")
        void createShipment_Success() throws Exception {
            given(shipmentService.createShipment(any(ShipmentCreateRequest.class)))
                    .willReturn(sampleShipmentResponse());

            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.trackingNumber").value("TRK-20260727-AB123"))
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(jsonPath("$.carrier").value("FedEx"))
                    .andExpect(jsonPath("$.trackingHistory.length()").value(1));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — creates shipment (201 Created)")
        void adminCreateShipment_Success() throws Exception {
            given(shipmentService.createShipment(any(ShipmentCreateRequest.class)))
                    .willReturn(sampleShipmentResponse());

            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — duplicate shipment returns 409 Conflict")
        void createShipment_DuplicatePrevention() throws Exception {
            given(shipmentService.createShipment(any(ShipmentCreateRequest.class)))
                    .willThrow(new ShipmentAlreadyExistsException(
                            "A shipment already exists for order with ID 10"
                    ));

            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("A shipment already exists for order with ID 10"));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — order not found returns 404")
        void createShipment_OrderNotFound() throws Exception {
            given(shipmentService.createShipment(any(ShipmentCreateRequest.class)))
                    .willThrow(new OrderNotFoundException("Order with ID 999 not found"));

            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — missing carrier returns 400 Bad Request")
        void createShipment_ValidationFail() throws Exception {
            ShipmentCreateRequest bad = new ShipmentCreateRequest(
                    10L, "", "Some Address", null, "Warehouse", null
            );
            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on create")
        void customerCreateShipment_Returns403() throws Exception {
            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on create")
        void unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/warehouse/shipments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Warehouse — List & Get Shipments
    // =========================================================================

    @Nested
    @DisplayName("GET /api/warehouse/shipments — List & Get Shipments")
    class WarehouseListShipments {

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — lists all shipments (200 OK)")
        void listAllShipments_Success() throws Exception {
            given(shipmentService.getAllShipments()).willReturn(List.of(sampleShipmentResponse()));

            mockMvc.perform(get("/api/warehouse/shipments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].trackingNumber").value("TRK-20260727-AB123"));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — gets shipment by ID (200 OK)")
        void getShipmentById_Success() throws Exception {
            given(shipmentService.getShipmentById(1L)).willReturn(sampleShipmentResponse());

            mockMvc.perform(get("/api/warehouse/shipments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("WAREHOUSE_STAFF — shipment not found returns 404")
        void getShipmentById_NotFound() throws Exception {
            given(shipmentService.getShipmentById(999L))
                    .willThrow(new ShipmentNotFoundException("Shipment with ID 999 not found"));

            mockMvc.perform(get("/api/warehouse/shipments/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // Warehouse — Status Transitions
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/warehouse/shipments/{id}/status — Status Transitions")
    class WarehouseShipmentStatusUpdate {

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("PROCESSING -> READY_TO_SHIP — valid transition (200 OK)")
        void validTransition_ProcessingToReadyToShip() throws Exception {
            ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest(
                    ShipmentStatus.READY_TO_SHIP, "Sorting Center - Chennai", "Package sorted and ready"
            );
            ShipmentResponse updated = new ShipmentResponse(
                    1L, "TRK-20260727-AB123", 10L, "ORD-2026-0001",
                    "FedEx", ShipmentStatus.READY_TO_SHIP,
                    "123 Main St, Chennai", null, null, null,
                    List.of(), LocalDateTime.now(), LocalDateTime.now()
            );
            given(shipmentService.updateShipmentStatus(eq(1L), any(ShipmentStatusUpdateRequest.class)))
                    .willReturn(updated);

            mockMvc.perform(patch("/api/warehouse/shipments/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY_TO_SHIP"));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("DELIVERED -> SHIPPED — invalid backward transition returns 400")
        void invalidTransition_DeliveredToShipped() throws Exception {
            ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest(
                    ShipmentStatus.SHIPPED, "Carrier Hub", "Attempting backward transition"
            );
            given(shipmentService.updateShipmentStatus(eq(1L), any(ShipmentStatusUpdateRequest.class)))
                    .willThrow(new InvalidShipmentStatusTransitionException(
                            "Cannot transition shipment from 'DELIVERED' to 'SHIPPED'. Allowed transitions: []"
                    ));

            mockMvc.perform(patch("/api/warehouse/shipments/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(
                            "Cannot transition shipment from 'DELIVERED' to 'SHIPPED'. Allowed transitions: []"
                    ));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("CANCELLED — valid cancellation before SHIPPED (200 OK)")
        void validCancellation_BeforeShipped() throws Exception {
            ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest(
                    ShipmentStatus.CANCELLED, "Warehouse", "Order cancelled by customer"
            );
            ShipmentResponse cancelled = new ShipmentResponse(
                    1L, "TRK-20260727-AB123", 10L, "ORD-2026-0001",
                    "FedEx", ShipmentStatus.CANCELLED,
                    "123 Main St", null, null, null,
                    List.of(), LocalDateTime.now(), LocalDateTime.now()
            );
            given(shipmentService.updateShipmentStatus(eq(1L), any(ShipmentStatusUpdateRequest.class)))
                    .willReturn(cancelled);

            mockMvc.perform(patch("/api/warehouse/shipments/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(username = "staff@shopstack.com", roles = {"WAREHOUSE_STAFF"})
        @DisplayName("SHIPPED -> CANCELLED — invalid cancellation after shipped returns 400")
        void invalidCancellation_AfterShipped() throws Exception {
            ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest(
                    ShipmentStatus.CANCELLED, "In Transit", "Trying to cancel after ship"
            );
            given(shipmentService.updateShipmentStatus(eq(1L), any(ShipmentStatusUpdateRequest.class)))
                    .willThrow(new InvalidShipmentStatusTransitionException(
                            "Cannot transition shipment from 'SHIPPED' to 'CANCELLED'. Allowed transitions: [OUT_FOR_DELIVERY]"
                    ));

            mockMvc.perform(patch("/api/warehouse/shipments/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // =========================================================================
    // Customer — Own Order Shipment Access
    // =========================================================================

    @Nested
    @DisplayName("GET /api/customer/shipments — Customer Shipment Access")
    class CustomerShipmentAccess {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — lists own shipments (200 OK)")
        void getMyShipments_Success() throws Exception {
            given(shipmentService.getCustomerShipments()).willReturn(List.of(sampleShipmentResponse()));

            mockMvc.perform(get("/api/customer/shipments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — gets shipment by own order ID (200 OK)")
        void getShipmentByOrderId_Success() throws Exception {
            given(shipmentService.getCustomerShipmentByOrderId(10L)).willReturn(sampleShipmentResponse());

            mockMvc.perform(get("/api/customer/shipments/order/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(10L));
        }

        @Test
        @WithMockUser(username = "customer2@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — cannot access another customer's order shipment (403)")
        void getShipmentByOrderId_Ownership403() throws Exception {
            given(shipmentService.getCustomerShipmentByOrderId(10L))
                    .willThrow(new ShipmentOwnershipException(
                            "Access denied: you do not own order with ID 10"
                    ));

            mockMvc.perform(get("/api/customer/shipments/order/10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Access denied: you do not own order with ID 10"));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on customer shipments")
        void unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/customer/shipments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 on customer shipment endpoint")
        void vendor_Returns403OnCustomerEndpoint() throws Exception {
            mockMvc.perform(get("/api/customer/shipments"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Vendor — Own Products Shipment Visibility
    // =========================================================================

    @Nested
    @DisplayName("GET /api/vendor/shipments — Vendor Shipment Visibility")
    class VendorShipmentAccess {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — lists shipments containing own products (200 OK)")
        void getVendorShipments_Success() throws Exception {
            given(shipmentService.getVendorShipments()).willReturn(List.of(sampleShipmentResponse()));

            mockMvc.perform(get("/api/vendor/shipments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on vendor shipments")
        void unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/vendor/shipments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 on vendor shipment endpoint")
        void customer_Returns403OnVendorEndpoint() throws Exception {
            mockMvc.perform(get("/api/vendor/shipments"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Tracking Number Lookup
    // =========================================================================

    @Nested
    @DisplayName("GET /api/shipments/tracking/{trackingNumber} — Tracking Lookup")
    class TrackingNumberLookup {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated — returns shipment by tracking number (200 OK)")
        void trackShipment_Success() throws Exception {
            given(shipmentService.getShipmentByTrackingNumber("TRK-20260727-AB123"))
                    .willReturn(sampleShipmentResponse());

            mockMvc.perform(get("/api/shipments/tracking/TRK-20260727-AB123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trackingNumber").value("TRK-20260727-AB123"))
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(jsonPath("$.trackingHistory.length()").value(1));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Invalid tracking number — returns 404 Not Found")
        void trackShipment_NotFound() throws Exception {
            given(shipmentService.getShipmentByTrackingNumber("TRK-INVALID"))
                    .willThrow(new ShipmentNotFoundException(
                            "Shipment with tracking number 'TRK-INVALID' not found"
                    ));

            mockMvc.perform(get("/api/shipments/tracking/TRK-INVALID"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on tracking lookup")
        void unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/shipments/tracking/TRK-20260727-AB123"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

package com.shopstack.controller;

import com.shopstack.dto.returns.RefundProcessRequest;
import com.shopstack.dto.returns.ReturnDecisionRequest;
import com.shopstack.dto.returns.ReturnRequestCreateRequest;
import com.shopstack.dto.returns.ReturnResponse;
import com.shopstack.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ReturnController — REST Controller handling return and refund lifecycle endpoints.
 *
 * <p>Customer endpoints: create return request, list own returns, view single return.
 * <p>Admin/Vendor endpoints: approve, reject, mark received, initiate refund, list all returns.
 */
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    // =========================================================================
    // CUSTOMER ENDPOINTS
    // =========================================================================

    /**
     * Creates a return request for a specific order.
     * POST /api/returns/orders/{orderId}
     */
    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReturnResponse> createReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestCreateRequest request
    ) {
        ReturnResponse response = returnService.createReturnRequest(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all return requests for the authenticated customer.
     * GET /api/returns/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> listMyReturns() {
        List<ReturnResponse> returns = returnService.listCustomerReturns();
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves a single return by its database ID.
     * GET /api/returns/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'VENDOR')")
    public ResponseEntity<ReturnResponse> getReturn(@PathVariable Long id) {
        ReturnResponse response = returnService.getReturn(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single return by its return number (e.g., RET-ABCD1234).
     * GET /api/returns/number/{returnNumber}
     */
    @GetMapping("/number/{returnNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'VENDOR')")
    public ResponseEntity<ReturnResponse> getReturnByNumber(@PathVariable String returnNumber) {
        ReturnResponse response = returnService.getReturnByNumber(returnNumber);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // =========================================================================
    // ADMIN / VENDOR / WAREHOUSE STAFF ENDPOINTS
    // =========================================================================

    /**
     * Lists all return requests across all customers.
     * GET /api/returns
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    public ResponseEntity<List<ReturnResponse>> listAllReturns() {
        List<ReturnResponse> returns = returnService.listAllReturns();
        return ResponseEntity.ok(returns);
    }

    /**
     * Approves a pending return request.
     * PUT /api/returns/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ReturnResponse> approveReturn(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReturnDecisionRequest request
    ) {
        ReturnDecisionRequest req = (request != null) ? request : new ReturnDecisionRequest();
        ReturnResponse response = returnService.approveReturn(id, req);
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a pending return request.
     * PUT /api/returns/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ReturnResponse> rejectReturn(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReturnDecisionRequest request
    ) {
        ReturnDecisionRequest req = (request != null) ? request : new ReturnDecisionRequest();
        ReturnResponse response = returnService.rejectReturn(id, req);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks the returned physical item as received (legacy endpoint).
     * PUT /api/returns/{id}/item-received
     */
    @PutMapping("/{id}/item-received")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ReturnResponse> markItemReceived(@PathVariable Long id) {
        ReturnResponse response = returnService.markItemReceived(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Warehouse Staff receives physical return and logs package condition.
     * PUT /api/returns/{id}/receive
     */
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<ReturnResponse> receiveReturn(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) com.shopstack.dto.returns.ReturnReceiveRequest request
    ) {
        com.shopstack.dto.returns.ReturnReceiveRequest req = (request != null) ? request : new com.shopstack.dto.returns.ReturnReceiveRequest();
        ReturnResponse response = returnService.receiveReturnAtWarehouse(id, req);
        return ResponseEntity.ok(response);
    }

    /**
     * Warehouse Staff performs Quality Check on received return.
     * PUT /api/returns/{id}/quality-check
     */
    @PutMapping("/{id}/quality-check")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<ReturnResponse> performQualityCheck(
            @PathVariable Long id,
            @Valid @RequestBody com.shopstack.dto.returns.QualityCheckRequest request
    ) {
        ReturnResponse response = returnService.performQualityCheck(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all returns assigned to a specific warehouse.
     * GET /api/returns/warehouse/{warehouseId}
     */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> getWarehouseReturns(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(returnService.getWarehouseReturns(warehouseId));
    }

    /**
     * Retrieves returns awaiting physical intake across all or specific warehouse.
     */
    @GetMapping({"/receiving", "/warehouse/receiving"})
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> getAllPendingReceivingReturns() {
        return ResponseEntity.ok(returnService.getPendingReceivingReturns(null));
    }

    @GetMapping("/warehouse/{warehouseId}/receiving")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> getPendingReceivingReturns(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(returnService.getPendingReceivingReturns(warehouseId));
    }

    /**
     * Retrieves returns awaiting Quality Check inspection across all or specific warehouse.
     */
    @GetMapping({"/qc", "/warehouse/qc"})
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> getAllPendingQcReturns() {
        return ResponseEntity.ok(returnService.getPendingQcReturns(null));
    }

    @GetMapping("/warehouse/{warehouseId}/qc")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<ReturnResponse>> getPendingQcReturns(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(returnService.getPendingQcReturns(warehouseId));
    }

    /**
     * Initiates a refund for a return with confirmed item receipt.
     * PUT /api/returns/{id}/initiate-refund
     */
    @PutMapping("/{id}/initiate-refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReturnResponse> initiateRefund(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RefundProcessRequest request
    ) {
        RefundProcessRequest req = (request != null) ? request : new RefundProcessRequest();
        ReturnResponse response = returnService.initiateRefund(id, req);
        return ResponseEntity.ok(response);
    }
}

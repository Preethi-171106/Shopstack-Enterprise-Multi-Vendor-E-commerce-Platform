package com.shopstack.service;

import com.shopstack.dto.returns.QualityCheckRequest;
import com.shopstack.dto.returns.RefundProcessRequest;
import com.shopstack.dto.returns.ReturnDecisionRequest;
import com.shopstack.dto.returns.ReturnReceiveRequest;
import com.shopstack.dto.returns.ReturnRequestCreateRequest;
import com.shopstack.dto.returns.ReturnResponse;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.NotificationType;
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
import com.shopstack.exception.InvalidReturnException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.ReturnNotFoundException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ReturnServiceImpl — Comprehensive service managing return requests, original warehouse routing,
 * receiving inspection, quality checking (accepted/damaged/quarantine), inventory adjustment, and refund processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RefundService refundService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final OrderItemWarehouseAllocationRepository allocationRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ReturnResponse createReturnRequest(Long orderId, ReturnRequestCreateRequest request) {
        User user = resolveAuthenticatedUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        // Verify ownership
        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to create a return for this order");
        }

        // Only DELIVERED orders are eligible for return
        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new ReturnNotAllowedException(
                    "Returns are only allowed for orders with DELIVERED status. Current status: " + order.getOrderStatus()
            );
        }

        // Resolve target order item
        OrderItem targetItem = null;
        if (request.getOrderItemId() != null) {
            targetItem = order.getItems().stream()
                    .filter(i -> i.getId().equals(request.getOrderItemId()))
                    .findFirst()
                    .orElse(null);
        }
        if (targetItem == null && order.getItems() != null && !order.getItems().isEmpty()) {
            targetItem = order.getItems().get(0);
        }

        int returnQty = 1;
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            returnQty = targetItem != null ? Math.min(request.getQuantity(), targetItem.getQuantity()) : request.getQuantity();
        } else if (targetItem != null) {
            returnQty = targetItem.getQuantity();
        }

        // Prevent duplicate active return requests for the same order item
        if (targetItem != null) {
            final Long targetItemId = targetItem.getId();
            boolean alreadyRequested = returnRequestRepository.findByOrderId(order.getId())
                    .map(r -> r.getOrderItem() != null && r.getOrderItem().getId().equals(targetItemId)
                            && r.getStatus() != ReturnStatus.REJECTED && r.getStatus() != ReturnStatus.CLOSED)
                    .orElse(false);
            if (alreadyRequested) {
                throw new InvalidReturnException("A return request has already been submitted for this item");
            }
        }

        // Resolve original fulfilling warehouse from OrderItemWarehouseAllocation
        Warehouse originalWh = null;
        if (targetItem != null) {
            List<OrderItemWarehouseAllocation> allocs = allocationRepository.findByOrderItemId(targetItem.getId());
            if (!allocs.isEmpty()) {
                originalWh = allocs.get(0).getWarehouse();
            }
        }
        if (originalWh == null) {
            List<OrderItemWarehouseAllocation> orderAllocs = allocationRepository.findByOrderId(order.getId());
            if (!orderAllocs.isEmpty()) {
                originalWh = orderAllocs.get(0).getWarehouse();
            }
        }

        // Designate return destination warehouse
        Warehouse returnWh = originalWh;
        if (returnWh == null || !returnWh.isActive()) {
            List<Warehouse> activeWhs = warehouseRepository.findByActiveTrue();
            if (!activeWhs.isEmpty()) {
                returnWh = activeWhs.get(0);
            }
        }

        // Calculate refund amount
        BigDecimal unitPrice = (targetItem != null && targetItem.getUnitPriceSnapshot() != null)
                ? targetItem.getUnitPriceSnapshot()
                : (targetItem != null && targetItem.getPrice() != null ? targetItem.getPrice() : order.getTotalAmount());
        BigDecimal refundAmount = unitPrice.multiply(BigDecimal.valueOf(returnQty)).min(order.getTotalAmount());

        String returnNumber = "RET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNumber(returnNumber)
                .order(order)
                .orderItem(targetItem)
                .returnQuantity(returnQty)
                .user(user)
                .originalWarehouse(originalWh)
                .returnWarehouse(returnWh)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReturnStatus.REQUESTED)
                .qcStatus("PENDING")
                .refundAmount(refundAmount)
                .requestedAt(LocalDateTime.now())
                .build();

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // Update order status
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        // Send notifications
        if (notificationService != null) {
            notificationService.send(
                    user,
                    NotificationType.RETURN_REQUESTED,
                    "Return Request Submitted",
                    "Your return request " + returnNumber + " for order #" + order.getOrderNumber() + " has been submitted and is under review.",
                    saved.getId()
            );
            notificationService.sendToRole(
                    UserRole.ADMIN,
                    NotificationType.RETURN_REQUESTED,
                    "New Return Request",
                    "A return request (" + returnNumber + ") was requested for order #" + order.getOrderNumber() + ".",
                    saved.getId()
            );
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnResponse approveReturn(Long id, ReturnDecisionRequest request) {
        ReturnRequest returnRequest = findReturnById(id);

        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw new InvalidReturnException("Cannot approve return in status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setApprovedAt(LocalDateTime.now());
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            returnRequest.setAdminNotes(request.getRemarks());
        }

        // Ensure return warehouse is set
        if (returnRequest.getReturnWarehouse() == null) {
            if (returnRequest.getOriginalWarehouse() != null && returnRequest.getOriginalWarehouse().isActive()) {
                returnRequest.setReturnWarehouse(returnRequest.getOriginalWarehouse());
            } else {
                List<Warehouse> active = warehouseRepository.findByActiveTrue();
                if (!active.isEmpty()) {
                    returnRequest.setReturnWarehouse(active.get(0));
                }
            }
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // Notify customer and warehouse staff
        if (notificationService != null) {
            if (saved.getUser() != null) {
                notificationService.send(
                        saved.getUser(),
                        NotificationType.RETURN_APPROVED,
                        "Return Request Approved",
                        "Your return request " + saved.getReturnNumber() + " for order #" + saved.getOrder().getOrderNumber() + " has been approved. Please ship items to " +
                                (saved.getReturnWarehouse() != null ? saved.getReturnWarehouse().getName() : "our fulfillment center") + ".",
                        saved.getId()
                );
            }
            notificationService.sendToRole(
                    UserRole.WAREHOUSE_STAFF,
                    NotificationType.RETURN_APPROVED,
                    "Incoming Return Approved",
                    "Return " + saved.getReturnNumber() + " approved for intake at " +
                            (saved.getReturnWarehouse() != null ? saved.getReturnWarehouse().getWarehouseCode() : "Warehouse"),
                    saved.getId()
            );
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnResponse rejectReturn(Long id, ReturnDecisionRequest request) {
        ReturnRequest returnRequest = findReturnById(id);

        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw new InvalidReturnException("Cannot reject return in status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setRejectedAt(LocalDateTime.now());
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            returnRequest.setAdminNotes(request.getRemarks());
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        if (notificationService != null && saved.getUser() != null) {
            notificationService.send(
                    saved.getUser(),
                    NotificationType.RETURN_REQUESTED,
                    "Return Request Rejected",
                    "Your return request " + saved.getReturnNumber() + " was not approved. Remarks: " +
                            (saved.getAdminNotes() != null ? saved.getAdminNotes() : "Items did not meet return policy criteria"),
                    saved.getId()
            );
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnResponse markItemReceived(Long id) {
        ReturnReceiveRequest req = ReturnReceiveRequest.builder()
                .packageCondition("Good")
                .receivingNotes("Standard intake receipt")
                .build();
        return receiveReturnAtWarehouse(id, req);
    }

    @Override
    @Transactional
    public ReturnResponse receiveReturnAtWarehouse(Long id, ReturnReceiveRequest request) {
        ReturnRequest returnRequest = findReturnById(id);

        if (returnRequest.getStatus() != ReturnStatus.APPROVED && returnRequest.getStatus() != ReturnStatus.IN_TRANSIT) {
            throw new InvalidReturnException("Cannot receive return in status: " + returnRequest.getStatus() + ". Expected status: APPROVED");
        }

        returnRequest.setStatus(ReturnStatus.ITEM_RECEIVED);
        returnRequest.setReturnReceivedDate(LocalDateTime.now());
        returnRequest.setQcStatus("QC_PENDING");

        if (request != null) {
            if (request.getPackageCondition() != null) returnRequest.setPackageCondition(request.getPackageCondition());
            if (request.getReceivingNotes() != null) returnRequest.setReceivingNotes(request.getReceivingNotes());
        }

        Warehouse wh = returnRequest.getReturnWarehouse();
        if (wh == null && returnRequest.getOriginalWarehouse() != null) {
            wh = returnRequest.getOriginalWarehouse();
            returnRequest.setReturnWarehouse(wh);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        User currentUser = resolveOptionalUser();

        // Record immutable StockMovement (RETURN_RECEIVED)
        Product product = (saved.getOrderItem() != null) ? saved.getOrderItem().getProduct() : null;
        if (product != null && wh != null) {
            Optional<WarehouseInventory> wiOpt = warehouseInventoryRepository.findByWarehouseIdAndProductId(wh.getId(), product.getId());
            int prevStock = wiOpt.map(WarehouseInventory::getTotalQuantity).orElse(0);

            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .warehouse(wh)
                    .movementType(StockMovementType.RETURN_RECEIVED)
                    .quantity(saved.getReturnQuantity())
                    .previousStock(prevStock)
                    .newStock(prevStock)
                    .referenceId(saved.getReturnNumber())
                    .notes("Physical return received at " + wh.getWarehouseCode() + ". Staged for Quality Inspection. Condition: " + (saved.getPackageCondition() != null ? saved.getPackageCondition() : "Received"))
                    .performedBy(currentUser)
                    .build();
            stockMovementRepository.save(movement);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnResponse performQualityCheck(Long id, QualityCheckRequest request) {
        ReturnRequest returnRequest = findReturnById(id);

        if (returnRequest.getStatus() != ReturnStatus.ITEM_RECEIVED && !"QC_PENDING".equalsIgnoreCase(returnRequest.getQcStatus())) {
            throw new InvalidReturnException("Cannot perform QC on return in status: " + returnRequest.getStatus());
        }

        User inspector = resolveAuthenticatedUser();
        returnRequest.setQcStatus("QC_COMPLETED");
        returnRequest.setQcResult(request.getResult());
        returnRequest.setQcNotes(request.getConditionNotes());
        returnRequest.setQcInspector(inspector);
        returnRequest.setQcCompletedAt(LocalDateTime.now());

        int totalQty = returnRequest.getReturnQuantity();
        int acceptedQty = 0;
        int damagedQty = 0;

        if (request.getResult() == QualityCheckResult.ACCEPTED) {
            acceptedQty = request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : totalQty;
            damagedQty = request.getDamagedQuantity() != null ? request.getDamagedQuantity() : 0;
        } else if (request.getResult() == QualityCheckResult.DAMAGED) {
            acceptedQty = request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : 0;
            damagedQty = request.getDamagedQuantity() != null ? request.getDamagedQuantity() : totalQty;
        } else if (request.getResult() == QualityCheckResult.PARTIALLY_ACCEPTED) {
            acceptedQty = request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : Math.max(0, totalQty - 1);
            damagedQty = request.getDamagedQuantity() != null ? request.getDamagedQuantity() : (totalQty - acceptedQty);
        } else { // REJECTED
            acceptedQty = 0;
            damagedQty = totalQty;
        }

        returnRequest.setAcceptedQuantity(acceptedQty);
        returnRequest.setDamagedQuantity(damagedQty);

        Warehouse returnWh = returnRequest.getReturnWarehouse();
        if (returnWh == null) {
            returnWh = returnRequest.getOriginalWarehouse();
        }
        if (returnWh == null) {
            List<Warehouse> activeWhs = warehouseRepository.findByActiveTrue();
            if (!activeWhs.isEmpty()) returnWh = activeWhs.get(0);
        }
        returnRequest.setReturnWarehouse(returnWh);

        Product product = (returnRequest.getOrderItem() != null) ? returnRequest.getOrderItem().getProduct() : null;

        // 1. Process Accepted Stock (Restock to sellable inventory)
        if (acceptedQty > 0 && product != null && returnWh != null) {
            WarehouseInventory wi = warehouseInventoryRepository.findByWarehouseIdAndProductId(returnWh.getId(), product.getId())
                    .orElseGet(() -> WarehouseInventory.builder()
                            .warehouse(returnRequest.getReturnWarehouse())
                            .product(product)
                            .totalQuantity(0)
                            .reservedQuantity(0)
                            .availableQuantity(0)
                            .lowStockThreshold(10)
                            .version(0)
                            .build());

            int prevTotal = wi.getTotalQuantity();
            wi.setTotalQuantity(prevTotal + acceptedQty);
            wi.recalculateAvailableQuantity();
            warehouseInventoryRepository.save(wi);

            // Record immutable StockMovement (RETURN_ACCEPTED)
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .warehouse(returnWh)
                    .movementType(StockMovementType.RETURN_ACCEPTED)
                    .quantity(acceptedQty)
                    .previousStock(prevTotal)
                    .newStock(wi.getTotalQuantity())
                    .referenceId(returnRequest.getReturnNumber())
                    .notes("QC Passed: Restocked " + acceptedQty + " units into " + returnWh.getWarehouseCode() + " available inventory.")
                    .performedBy(inspector)
                    .build();
            stockMovementRepository.save(movement);

            // Sync global product & inventory
            syncGlobalInventory(product);
        }

        // 2. Process Damaged / Quarantine Stock (Completely isolated from sellable inventory)
        if (damagedQty > 0 && product != null && returnWh != null) {
            WarehouseInventory wi = warehouseInventoryRepository.findByWarehouseIdAndProductId(returnWh.getId(), product.getId())
                    .orElseGet(() -> WarehouseInventory.builder()
                            .warehouse(returnRequest.getReturnWarehouse())
                            .product(product)
                            .totalQuantity(0)
                            .reservedQuantity(0)
                            .availableQuantity(0)
                            .lowStockThreshold(10)
                            .version(0)
                            .build());

            boolean isQuarantine = Boolean.TRUE.equals(request.getMoveToQuarantine());
            if (isQuarantine) {
                wi.setQuarantineQuantity(wi.getQuarantineQuantity() + damagedQty);
            } else {
                wi.setDamagedQuantity(wi.getDamagedQuantity() + damagedQty);
            }
            warehouseInventoryRepository.save(wi);

            StockMovementType mType = isQuarantine ? StockMovementType.QUARANTINE : StockMovementType.RETURN_DAMAGED;
            StockMovement damageMovement = StockMovement.builder()
                    .product(product)
                    .warehouse(returnWh)
                    .movementType(mType)
                    .quantity(damagedQty)
                    .previousStock(wi.getTotalQuantity())
                    .newStock(wi.getTotalQuantity())
                    .referenceId(returnRequest.getReturnNumber())
                    .notes("QC Defect: " + damagedQty + " units isolated to " + (isQuarantine ? "QUARANTINE" : "DAMAGED") + " stock at " + returnWh.getWarehouseCode() + ". Reason: " + (request.getConditionNotes() != null ? request.getConditionNotes() : "Damaged upon receipt"))
                    .performedBy(inspector)
                    .build();
            stockMovementRepository.save(damageMovement);
        }

        // 3. Process Refund for Accepted Units
        Order order = returnRequest.getOrder();
        if (acceptedQty > 0) {
            BigDecimal refundAmt = returnRequest.getRefundAmount();
            if (totalQty > 0) {
                refundAmt = returnRequest.getRefundAmount()
                        .multiply(BigDecimal.valueOf(acceptedQty))
                        .divide(BigDecimal.valueOf(totalQty), 2, java.math.RoundingMode.HALF_UP);
            }
            returnRequest.setRefundAmount(refundAmt);

            try {
                Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(() -> {
                    Payment p = Payment.builder()
                            .order(order)
                            .paymentId("PAY-ORD-" + order.getOrderNumber())
                            .amount(order.getTotalAmount())
                            .currency("INR")
                            .paymentMethod(com.shopstack.entity.PaymentMethod.COD)
                            .gateway(com.shopstack.entity.PaymentGateway.RAZORPAY)
                            .status(com.shopstack.entity.PaymentStatus.SUCCESS)
                            .build();
                    return paymentRepository.save(p);
                });
                refundService.createRefundRecord(payment, returnRequest, "Refund for approved return (" + acceptedQty + " accepted items)");
            } catch (Exception e) {
                log.warn("Could not automatically create payment refund record for return {}: {}", returnRequest.getReturnNumber(), e.getMessage());
            }

            returnRequest.setStatus(ReturnStatus.REFUNDED);
            returnRequest.setCompletedAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.REFUNDED);
        } else {
            returnRequest.setStatus(ReturnStatus.CLOSED);
            returnRequest.setCompletedAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.RETURNED);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        orderRepository.save(order);

        // Notifications
        if (notificationService != null && saved.getUser() != null) {
            if (acceptedQty > 0) {
                notificationService.send(
                        saved.getUser(),
                        NotificationType.REFUND_PROCESSED,
                        "Return Inspection Passed & Refund Processed",
                        "Quality check passed for " + saved.getReturnNumber() + ". Refund of ₹" + saved.getRefundAmount() + " has been processed.",
                        saved.getId()
                );
            } else {
                notificationService.send(
                        saved.getUser(),
                        NotificationType.RETURN_REQUESTED,
                        "Return Quality Check Concluded",
                        "Returned items for " + saved.getReturnNumber() + " were marked as damaged/unacceptable. " + (saved.getQcNotes() != null ? saved.getQcNotes() : ""),
                        saved.getId()
                );
            }
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnResponse initiateRefund(Long id, RefundProcessRequest request) {
        ReturnRequest returnRequest = findReturnById(id);

        returnRequest.setStatus(ReturnStatus.REFUND_INITIATED);
        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        Payment payment = paymentRepository.findByOrderId(returnRequest.getOrder().getId())
                .orElseThrow(() -> new RuntimeException("Payment not found for order"));

        refundService.createRefundRecord(payment, saved, request != null ? request.getRemarks() : "Refund initiated for return");

        saved.setStatus(ReturnStatus.REFUNDED);
        saved.setCompletedAt(LocalDateTime.now());
        ReturnRequest finalized = returnRequestRepository.save(saved);

        Order order = finalized.getOrder();
        order.setOrderStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        if (notificationService != null && finalized.getUser() != null) {
            notificationService.send(
                    finalized.getUser(),
                    NotificationType.REFUND_PROCESSED,
                    "Refund Processed",
                    "A refund of ₹" + finalized.getRefundAmount() + " has been processed for return " + finalized.getReturnNumber() + ".",
                    finalized.getId()
            );
        }

        return toResponse(finalized);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturn(Long id) {
        return toResponse(findReturnById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturnByNumber(String returnNumber) {
        ReturnRequest returnRequest = returnRequestRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new ReturnNotFoundException("Return with number " + returnNumber + " not found"));
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> listCustomerReturns() {
        User user = resolveAuthenticatedUser();
        return returnRequestRepository.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> listAllReturns() {
        return returnRequestRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getWarehouseReturns(Long warehouseId) {
        List<ReturnRequest> list = (warehouseId != null)
                ? returnRequestRepository.findByReturnWarehouseId(warehouseId)
                : returnRequestRepository.findAll();
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getPendingReceivingReturns(Long warehouseId) {
        List<ReturnRequest> list = (warehouseId != null)
                ? returnRequestRepository.findByReturnWarehouseIdAndStatus(warehouseId, ReturnStatus.APPROVED)
                : returnRequestRepository.findByStatus(ReturnStatus.APPROVED);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getPendingQcReturns(Long warehouseId) {
        List<ReturnRequest> list = (warehouseId != null)
                ? returnRequestRepository.findByReturnWarehouseIdAndStatus(warehouseId, ReturnStatus.ITEM_RECEIVED)
                : returnRequestRepository.findByStatus(ReturnStatus.ITEM_RECEIVED);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void syncGlobalInventory(Product product) {
        List<WarehouseInventory> allWhInv = warehouseInventoryRepository.findByProductId(product.getId());
        int sumTotal = allWhInv.stream().mapToInt(WarehouseInventory::getTotalQuantity).sum();
        int sumReserved = allWhInv.stream().mapToInt(WarehouseInventory::getReservedQuantity).sum();

        Optional<Inventory> globalOpt = inventoryRepository.findByProductId(product.getId());
        Inventory globalInv = globalOpt.orElseGet(() -> Inventory.builder()
                .product(product)
                .totalStock(sumTotal)
                .reservedStock(sumReserved)
                .availableStock(Math.max(0, sumTotal - sumReserved))
                .lowStockThreshold(10)
                .version(0)
                .build());

        globalInv.setTotalStock(sumTotal);
        globalInv.setReservedStock(sumReserved);
        globalInv.recalculateAvailableStock();
        inventoryRepository.save(globalInv);

        product.setStockQuantity(sumTotal);
        productRepository.save(product);
    }

    private ReturnRequest findReturnById(Long id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new ReturnNotFoundException("Return with ID " + id + " not found"));
    }

    private User resolveAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private User resolveOptionalUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
                return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ReturnResponse toResponse(ReturnRequest r) {
        OrderItem item = r.getOrderItem();
        Product product = (item != null) ? item.getProduct() : null;
        Warehouse origWh = r.getOriginalWarehouse();
        Warehouse retWh = r.getReturnWarehouse();
        User cust = r.getUser();
        User inspector = r.getQcInspector();

        return ReturnResponse.builder()
                .id(r.getId())
                .returnNumber(r.getReturnNumber())
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .orderNumber(r.getOrder() != null ? r.getOrder().getOrderNumber() : null)
                .orderItemId(item != null ? item.getId() : null)
                .productId(product != null ? product.getId() : null)
                .productName(item != null && item.getProductNameSnapshot() != null ? item.getProductNameSnapshot() : (product != null ? product.getName() : null))
                .productSku(product != null ? product.getSku() : null)
                .productImageUrl(item != null && item.getProductImageUrlSnapshot() != null ? item.getProductImageUrlSnapshot() : (product != null ? product.getImageUrl() : null))
                .returnQuantity(r.getReturnQuantity())
                .userId(cust != null ? cust.getId() : null)
                .userEmail(cust != null ? cust.getEmail() : null)
                .customerName(cust != null ? (cust.getFirstName() + " " + (cust.getLastName() != null ? cust.getLastName() : "")).trim() : null)
                .originalWarehouseId(origWh != null ? origWh.getId() : null)
                .originalWarehouseCode(origWh != null ? origWh.getWarehouseCode() : null)
                .originalWarehouseName(origWh != null ? origWh.getName() : null)
                .returnWarehouseId(retWh != null ? retWh.getId() : null)
                .returnWarehouseCode(retWh != null ? retWh.getWarehouseCode() : null)
                .returnWarehouseName(retWh != null ? retWh.getName() : null)
                .reason(r.getReason())
                .description(r.getDescription())
                .adminNotes(r.getAdminNotes())
                .status(r.getStatus())
                .refundAmount(r.getRefundAmount())
                .requestedAt(r.getRequestedAt())
                .approvedAt(r.getApprovedAt())
                .rejectedAt(r.getRejectedAt())
                .returnReceivedDate(r.getReturnReceivedDate())
                .packageCondition(r.getPackageCondition())
                .receivingNotes(r.getReceivingNotes())
                .qcStatus(r.getQcStatus())
                .qcResult(r.getQcResult())
                .qcNotes(r.getQcNotes())
                .qcInspectorName(inspector != null ? (inspector.getFirstName() + " " + (inspector.getLastName() != null ? inspector.getLastName() : "")).trim() : null)
                .qcCompletedAt(r.getQcCompletedAt())
                .acceptedQuantity(r.getAcceptedQuantity())
                .damagedQuantity(r.getDamagedQuantity())
                .completedAt(r.getCompletedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}

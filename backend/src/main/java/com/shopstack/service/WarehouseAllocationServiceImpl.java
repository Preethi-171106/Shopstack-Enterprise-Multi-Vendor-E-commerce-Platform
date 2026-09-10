package com.shopstack.service;

import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.dto.warehouse.AllocationResponse;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.Product;
import com.shopstack.entity.Shipment;
import com.shopstack.entity.ShipmentStatus;
import com.shopstack.entity.StockAllocationStatus;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.TrackingEvent;
import com.shopstack.entity.User;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.WarehouseAllocationException;
import com.shopstack.mapper.AllocationMapper;
import com.shopstack.repository.OrderItemWarehouseAllocationRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ShipmentRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseAllocationServiceImpl implements WarehouseAllocationService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final OrderItemWarehouseAllocationRepository allocationRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AllocationMapper allocationMapper;

    @Override
    @Transactional
    public List<AllocationResponse> allocateOrderItems(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return List.of();
        }

        // Idempotency check: Return existing allocations if already allocated for this order
        if (order.getId() != null) {
            List<OrderItemWarehouseAllocation> existingOrderAllocs = allocationRepository.findByOrderId(order.getId());
            if (!existingOrderAllocs.isEmpty()) {
                log.info("Order #{} already has {} allocations created. Returning existing allocations.",
                        order.getOrderNumber(), existingOrderAllocs.size());
                return existingOrderAllocs.stream()
                        .map(allocationMapper::toResponse)
                        .collect(Collectors.toList());
            }
        }

        List<OrderItemWarehouseAllocation> createdAllocations = new ArrayList<>();
        User currentUser = resolveOptionalUser();

        // Check if a single active warehouse can satisfy ALL items in this order
        List<Warehouse> activeWarehouses = warehouseRepository.findByActiveTrue();
        Warehouse preferredSingleWarehouse = null;

        for (Warehouse candidate : activeWarehouses) {
            boolean canFulfillAll = true;
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null || item.getQuantity() <= 0) continue;
                Optional<WarehouseInventory> wiOpt = warehouseInventoryRepository
                        .findByWarehouseIdAndProductId(candidate.getId(), item.getProduct().getId());
                if (wiOpt.isEmpty() || wiOpt.get().getAvailableQuantity() < item.getQuantity()) {
                    canFulfillAll = false;
                    break;
                }
            }
            if (canFulfillAll) {
                preferredSingleWarehouse = candidate;
                break;
            }
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int requestedQty = item.getQuantity();

            if (product == null || requestedQty <= 0) {
                continue;
            }

            WarehouseInventory selectedInventory = null;

            if (preferredSingleWarehouse != null) {
                selectedInventory = warehouseInventoryRepository
                        .findByWarehouseIdAndProductId(preferredSingleWarehouse.getId(), product.getId())
                        .orElse(null);
            }

            if (selectedInventory == null || selectedInventory.getAvailableQuantity() < requestedQty) {
                // Step 1 & 2: Find all active warehouses with available stock for this product
                List<WarehouseInventory> eligibleInventories = warehouseInventoryRepository
                        .findEligibleWarehousesForProduct(product.getId(), requestedQty);

                if (!eligibleInventories.isEmpty()) {
                    // Priority selection (Sorted by availableQuantity DESC, warehouse.id ASC)
                    selectedInventory = eligibleInventories.get(0);
                } else {
                    // Fallback: Check if primary or any active warehouse has the SKU or initialize it if sufficient global stock exists
                    List<WarehouseInventory> allWhInvs = warehouseInventoryRepository.findByProductIdAndWarehouseActiveTrue(product.getId());
                    if (!allWhInvs.isEmpty()) {
                        selectedInventory = allWhInvs.get(0);
                    } else {
                        if (activeWarehouses.isEmpty()) {
                            throw new WarehouseAllocationException("No active warehouses available in the system for fulfillment.");
                        }
                        Warehouse primaryWarehouse = activeWarehouses.get(0);
                        selectedInventory = warehouseInventoryRepository.findByWarehouseIdAndProductId(primaryWarehouse.getId(), product.getId())
                                .orElseGet(() -> {
                                    WarehouseInventory wi = WarehouseInventory.builder()
                                            .warehouse(primaryWarehouse)
                                            .product(product)
                                            .totalQuantity(Math.max(product.getStockQuantity(), requestedQty))
                                            .reservedQuantity(0)
                                            .availableQuantity(Math.max(product.getStockQuantity(), requestedQty))
                                            .lowStockThreshold(10)
                                            .version(0)
                                            .build();
                                    wi.recalculateAvailableQuantity();
                                    return warehouseInventoryRepository.save(wi);
                                });
                    }
                }
            }

            if (selectedInventory.getAvailableQuantity() < requestedQty) {
                log.warn("Warehouse {} has insufficient stock for product {} (Required: {}, Available: {}). Auto-adjusting for fulfillment.",
                        selectedInventory.getWarehouse().getWarehouseCode(), product.getName(), requestedQty, selectedInventory.getAvailableQuantity());
                if (selectedInventory.getTotalQuantity() < selectedInventory.getReservedQuantity() + requestedQty) {
                    selectedInventory.setTotalQuantity(selectedInventory.getReservedQuantity() + requestedQty);
                }
            }

            // Step 4: Reserve Stock at Warehouse Inventory Level
            int prevTotal = selectedInventory.getTotalQuantity();
            int prevReserved = selectedInventory.getReservedQuantity();
            selectedInventory.setReservedQuantity(prevReserved + requestedQty);
            selectedInventory.recalculateAvailableQuantity();
            warehouseInventoryRepository.save(selectedInventory);

            // Step 5: Create OrderItemWarehouseAllocation record
            OrderItemWarehouseAllocation allocation = OrderItemWarehouseAllocation.builder()
                    .orderItem(item)
                    .warehouse(selectedInventory.getWarehouse())
                    .warehouseInventory(selectedInventory)
                    .allocatedQuantity(requestedQty)
                    .allocationStatus(StockAllocationStatus.ALLOCATED)
                    .allocatedAt(LocalDateTime.now())
                    .notes("Allocated to " + selectedInventory.getWarehouse().getName() + " for order #" + order.getOrderNumber())
                    .build();

            OrderItemWarehouseAllocation savedAlloc = allocationRepository.save(allocation);
            createdAllocations.add(savedAlloc);

            // Step 6: Log Stock Movement Audit Record (ALLOCATED)
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .warehouse(selectedInventory.getWarehouse())
                    .movementType(StockMovementType.ALLOCATED)
                    .quantity(requestedQty)
                    .previousStock(prevTotal)
                    .newStock(prevTotal)
                    .referenceId("ORD-" + order.getOrderNumber())
                    .notes("Stock allocated & reserved at " + selectedInventory.getWarehouse().getWarehouseCode() + " for order item #" + item.getId())
                    .performedBy(currentUser)
                    .build();
            stockMovementRepository.save(movement);
        }

        return createdAllocations.stream()
                .map(allocationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationResponse> getAllocations(Long warehouseId, String status) {
        List<OrderItemWarehouseAllocation> list;
        if (warehouseId != null) {
            list = allocationRepository.findByWarehouseIdWithDetails(warehouseId);
        } else {
            list = allocationRepository.findAllWithDetails();
        }

        return list.stream()
                .filter(a -> {
                    if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    return a.getAllocationStatus().name().equalsIgnoreCase(status.trim());
                })
                .map(allocationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationResponse> getPickingAllocations(Long warehouseId) {
        return getAllocations(warehouseId, StockAllocationStatus.ALLOCATED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationResponse> getPackingAllocations(Long warehouseId) {
        return getAllocations(warehouseId, StockAllocationStatus.PICKED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationResponse> getReadyToShipAllocations(Long warehouseId) {
        return getAllocations(warehouseId, StockAllocationStatus.PACKED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public AllocationResponse getAllocationById(Long allocationId) {
        OrderItemWarehouseAllocation alloc = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new WarehouseAllocationException("Allocation with ID " + allocationId + " not found"));
        return allocationMapper.toResponse(alloc);
    }

    @Override
    @Transactional
    public AllocationResponse pickAllocation(Long allocationId) {
        OrderItemWarehouseAllocation alloc = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new WarehouseAllocationException("Allocation with ID " + allocationId + " not found"));

        if (alloc.getAllocationStatus() != StockAllocationStatus.ALLOCATED) {
            throw new WarehouseAllocationException("Cannot pick allocation with status: " + alloc.getAllocationStatus() +
                    ". Expected status: " + StockAllocationStatus.ALLOCATED);
        }

        alloc.setAllocationStatus(StockAllocationStatus.PICKED);
        alloc.setPickedAt(LocalDateTime.now());
        OrderItemWarehouseAllocation saved = allocationRepository.save(alloc);

        User currentUser = resolveOptionalUser();
        WarehouseInventory wi = alloc.getWarehouseInventory();
        int stockTotal = wi != null ? wi.getTotalQuantity() : 0;

        // Log StockMovement of type PICKED
        StockMovement movement = StockMovement.builder()
                .product(alloc.getOrderItem().getProduct())
                .warehouse(alloc.getWarehouse())
                .movementType(StockMovementType.PICKED)
                .quantity(alloc.getAllocatedQuantity())
                .previousStock(stockTotal)
                .newStock(stockTotal)
                .referenceId("PICK-ALLOC-" + alloc.getId())
                .notes("Picked " + alloc.getAllocatedQuantity() + " units at " + alloc.getWarehouse().getWarehouseCode() + " for order #" + alloc.getOrderItem().getOrder().getOrderNumber())
                .performedBy(currentUser)
                .build();
        stockMovementRepository.save(movement);

        // Update Shipment tracking if order has shipment
        Order order = alloc.getOrderItem().getOrder();
        Shipment shipment = getOrCreateShipment(order);
        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING,
                alloc.getWarehouse().getName() + " (Pick Station)",
                "Picked " + alloc.getAllocatedQuantity() + "x " +
                        (alloc.getOrderItem().getProductNameSnapshot() != null ? alloc.getOrderItem().getProductNameSnapshot() : "Item") +
                        " from Bay at " + alloc.getWarehouse().getWarehouseCode());

        return allocationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AllocationResponse packAllocation(Long allocationId, AllocationActionRequest request) {
        OrderItemWarehouseAllocation alloc = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new WarehouseAllocationException("Allocation with ID " + allocationId + " not found"));

        if (alloc.getAllocationStatus() != StockAllocationStatus.PICKED) {
            throw new WarehouseAllocationException("Cannot pack allocation with status: " + alloc.getAllocationStatus() +
                    ". Expected status: " + StockAllocationStatus.PICKED);
        }

        alloc.setAllocationStatus(StockAllocationStatus.PACKED);
        alloc.setPackedAt(LocalDateTime.now());
        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            alloc.setNotes(request.getNotes());
        }
        OrderItemWarehouseAllocation saved = allocationRepository.save(alloc);

        User currentUser = resolveOptionalUser();
        WarehouseInventory wi = alloc.getWarehouseInventory();
        int stockTotal = wi != null ? wi.getTotalQuantity() : 0;

        // Log StockMovement of type PACKED
        StockMovement movement = StockMovement.builder()
                .product(alloc.getOrderItem().getProduct())
                .warehouse(alloc.getWarehouse())
                .movementType(StockMovementType.PACKED)
                .quantity(alloc.getAllocatedQuantity())
                .previousStock(stockTotal)
                .newStock(stockTotal)
                .referenceId("PACK-ALLOC-" + alloc.getId())
                .notes("Packed " + alloc.getAllocatedQuantity() + " units at " + alloc.getWarehouse().getWarehouseCode() + " for order #" + alloc.getOrderItem().getOrder().getOrderNumber())
                .performedBy(currentUser)
                .build();
        stockMovementRepository.save(movement);

        // Update Shipment tracking
        Order order = alloc.getOrderItem().getOrder();
        Shipment shipment = getOrCreateShipment(order);
        String packDetail = "Packed & sealed item (" + alloc.getAllocatedQuantity() + " units) at " + alloc.getWarehouse().getWarehouseCode();
        if (request != null) {
            if (request.getPackageWeight() != null) packDetail += " | Wt: " + request.getPackageWeight();
            if (request.getPackageDimensions() != null) packDetail += " | Dim: " + request.getPackageDimensions();
        }
        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING, alloc.getWarehouse().getName() + " (Packing Bay)", packDetail);

        return allocationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AllocationResponse readyForShipment(Long allocationId, AllocationActionRequest request) {
        OrderItemWarehouseAllocation alloc = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new WarehouseAllocationException("Allocation with ID " + allocationId + " not found"));

        if (alloc.getAllocationStatus() != StockAllocationStatus.PACKED) {
            throw new WarehouseAllocationException("Cannot mark ready for shipment with status: " + alloc.getAllocationStatus() +
                    ". Expected status: " + StockAllocationStatus.PACKED);
        }

        alloc.setAllocationStatus(StockAllocationStatus.READY_FOR_SHIPMENT);
        alloc.setReadyForShipmentAt(LocalDateTime.now());
        OrderItemWarehouseAllocation saved = allocationRepository.save(alloc);

        User currentUser = resolveOptionalUser();
        WarehouseInventory wi = alloc.getWarehouseInventory();
        int stockTotal = wi != null ? wi.getTotalQuantity() : 0;

        // Log StockMovement of type READY_FOR_SHIPMENT
        StockMovement movement = StockMovement.builder()
                .product(alloc.getOrderItem().getProduct())
                .warehouse(alloc.getWarehouse())
                .movementType(StockMovementType.READY_FOR_SHIPMENT)
                .quantity(alloc.getAllocatedQuantity())
                .previousStock(stockTotal)
                .newStock(stockTotal)
                .referenceId("READY-ALLOC-" + alloc.getId())
                .notes("Staged for shipment at " + alloc.getWarehouse().getWarehouseCode() + " for order #" + alloc.getOrderItem().getOrder().getOrderNumber())
                .performedBy(currentUser)
                .build();
        stockMovementRepository.save(movement);

        // Check if all allocations for this order are READY_FOR_SHIPMENT
        Order order = alloc.getOrderItem().getOrder();
        List<OrderItemWarehouseAllocation> allOrderAllocs = allocationRepository.findByOrderId(order.getId());
        boolean allReady = allOrderAllocs.stream()
                .allMatch(a -> a.getAllocationStatus() == StockAllocationStatus.READY_FOR_SHIPMENT);

        Shipment shipment = getOrCreateShipment(order);
        if (request != null && request.getCarrier() != null && !request.getCarrier().isBlank()) {
            shipment.setCarrier(request.getCarrier().trim());
        }
        if (request != null && request.getEstimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }

        if (allReady) {
            shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
            appendTrackingEvent(shipment, ShipmentStatus.READY_TO_SHIP,
                    alloc.getWarehouse().getName() + " (Staging Bay)",
                    "All items packed, labeled, and staged in Dispatch Bay (Carrier: " + shipment.getCarrier() + ")");
            shipmentRepository.save(shipment);

            if (order.getUser() != null) {
                notificationService.send(
                        order.getUser(),
                        NotificationType.ORDER_PROCESSING,
                        "Order Ready for Dispatch",
                        "Your order #" + order.getOrderNumber() + " is packed and staged for carrier pickup with " + shipment.getCarrier() + ". Tracking: " + shipment.getTrackingNumber(),
                        order.getId()
                );
            }
        } else {
            appendTrackingEvent(shipment, ShipmentStatus.PROCESSING,
                    alloc.getWarehouse().getName() + " (Staging Bay)",
                    "Item staged for dispatch at " + alloc.getWarehouse().getWarehouseCode());
            shipmentRepository.save(shipment);
        }

        return allocationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void releaseOrderAllocations(Long orderId) {
        List<OrderItemWarehouseAllocation> allocations = allocationRepository.findByOrderId(orderId);
        User currentUser = resolveOptionalUser();

        for (OrderItemWarehouseAllocation alloc : allocations) {
            WarehouseInventory wi = alloc.getWarehouseInventory();
            if (wi != null) {
                int qty = alloc.getAllocatedQuantity();
                wi.setReservedQuantity(Math.max(0, wi.getReservedQuantity() - qty));
                wi.recalculateAvailableQuantity();
                warehouseInventoryRepository.save(wi);

                StockMovement movement = StockMovement.builder()
                        .product(alloc.getOrderItem().getProduct())
                        .warehouse(alloc.getWarehouse())
                        .movementType(StockMovementType.RELEASED)
                        .quantity(qty)
                        .previousStock(wi.getTotalQuantity())
                        .newStock(wi.getTotalQuantity())
                        .referenceId("CANCEL-ORD-" + orderId)
                        .notes("Reserved stock released due to order cancellation")
                        .performedBy(currentUser)
                        .build();
                stockMovementRepository.save(movement);
            }
            allocationRepository.delete(alloc);
        }
    }

    private Shipment getOrCreateShipment(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }
        Optional<Shipment> existing = shipmentRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String trackingNumber = generateUniqueTrackingNumber();
        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .order(order)
                .carrier("ShopStack Express Logistics")
                .status(ShipmentStatus.PROCESSING)
                .shippingAddress(order.getShippingAddress() != null ? order.getShippingAddress() : "Customer Address")
                .build();

        TrackingEvent initialEvent = TrackingEvent.builder()
                .shipment(shipment)
                .status(ShipmentStatus.PROCESSING)
                .location("Fulfillment Network")
                .description("Order received for multi-warehouse allocation and fulfillment")
                .build();
        shipment.getTrackingEvents().add(initialEvent);

        Shipment saved = shipmentRepository.save(shipment);
        return saved != null ? saved : shipment;
    }

    private void appendTrackingEvent(Shipment shipment, ShipmentStatus status, String location, String description) {
        if (shipment == null) {
            return;
        }
        if (shipment.getTrackingEvents() == null) {
            shipment.setTrackingEvents(new ArrayList<>());
        }
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .description(description)
                .build();
        shipment.getTrackingEvents().add(event);
        shipmentRepository.save(shipment);
    }

    private String generateUniqueTrackingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = String.format("%05X", random.nextInt(0xFFFFF));
            String candidate = "TRK-" + date + "-" + suffix;
            if (!shipmentRepository.existsByTrackingNumber(candidate)) {
                return candidate;
            }
        }
        return "TRK-" + date + "-" + System.currentTimeMillis();
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
}

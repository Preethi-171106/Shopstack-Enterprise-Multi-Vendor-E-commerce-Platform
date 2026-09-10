package com.shopstack.service;

import com.shopstack.dto.shipment.ShipmentCreateRequest;
import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.dto.shipment.ShipmentStatusUpdateRequest;
import com.shopstack.dto.warehouse.WarehouseDashboardResponse;
import com.shopstack.dto.warehouse.WarehouseOrderItemResponse;
import com.shopstack.dto.warehouse.WarehouseOrderResponse;
import com.shopstack.dto.warehouse.WarehousePackingRequest;
import com.shopstack.dto.warehouse.WarehouseReadyToShipRequest;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Shipment;
import com.shopstack.entity.ShipmentStatus;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.TrackingEvent;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.ShipmentNotFoundException;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ShipmentRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public WarehouseDashboardResponse getWarehouseDashboard() {
        List<Order> allOrders = orderRepository.findAll();
        List<Shipment> allShipments = shipmentRepository.findAll();
        List<Inventory> allInventories = inventoryRepository.findAll();

        long processingOrders = 0;
        long readyForPicking = 0;
        long inPicking = 0;
        long readyForPacking = 0;
        long packed = 0;
        long readyToShip = 0;
        long activeShipments = 0;
        long deliveredToday = 0;

        LocalDate today = LocalDate.now();

        for (Shipment s : allShipments) {
            if (s.getStatus() == ShipmentStatus.READY_TO_SHIP) {
                readyToShip++;
            }
            if (s.getStatus() != ShipmentStatus.DELIVERED &&
                s.getStatus() != ShipmentStatus.CANCELLED &&
                s.getStatus() != ShipmentStatus.RETURNED) {
                activeShipments++;
            }
            if (s.getStatus() == ShipmentStatus.DELIVERED && s.getDeliveredDate() != null
                    && s.getDeliveredDate().toLocalDate().isEqual(today)) {
                deliveredToday++;
            }
        }

        List<WarehouseOrderResponse> urgentOrderResponses = new ArrayList<>();

        for (Order o : allOrders) {
            if (o.getOrderStatus() == OrderStatus.PROCESSING || o.getOrderStatus() == OrderStatus.CONFIRMED) {
                processingOrders++;
                String wStatus = deriveWarehouseStatus(o);
                switch (wStatus) {
                    case "READY_FOR_PICKING" -> readyForPicking++;
                    case "PICKING" -> inPicking++;
                    case "PICKED", "READY_FOR_PACKING" -> readyForPacking++;
                    case "PACKED" -> packed++;
                }

                if (urgentOrderResponses.size() < 10) {
                    urgentOrderResponses.add(mapToWarehouseOrderResponse(o));
                }
            }
        }

        long totalStockQty = allInventories.stream().mapToLong(Inventory::getTotalStock).sum();
        long lowStockCount = allInventories.stream().filter(Inventory::isLowStock).count();
        long outOfStockCount = allInventories.stream().filter(inv -> inv.getAvailableStock() <= 0 || inv.getTotalStock() <= 0).count();

        long movementsToday = 0;
        try {
            movementsToday = stockMovementRepository.findAll().stream()
                    .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().toLocalDate().isEqual(today))
                    .count();
        } catch (Exception ignored) {}

        return WarehouseDashboardResponse.builder()
                .totalTrackedInventories(allInventories.size())
                .totalStockQuantity(totalStockQty)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .totalProcessingOrders(processingOrders)
                .readyForPickingOrders(readyForPicking)
                .inPickingOrders(inPicking)
                .readyForPackingOrders(readyForPacking)
                .packedOrders(packed)
                .readyToShipShipments(readyToShip)
                .activeShipments(activeShipments)
                .deliveredToday(deliveredToday)
                .totalStockMovementsToday(movementsToday)
                .urgentOrders(urgentOrderResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseOrderResponse> getWarehouseOrders(String statusFilter) {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(this::mapToWarehouseOrderResponse)
                .filter(wo -> {
                    if (statusFilter == null || statusFilter.isBlank() || statusFilter.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    return wo.getWarehouseStatus().equalsIgnoreCase(statusFilter)
                            || wo.getOrderStatus().equalsIgnoreCase(statusFilter);
                })
                .sorted(Comparator.comparing(WarehouseOrderResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseOrderResponse> getPickingOrders() {
        return getWarehouseOrders("ALL").stream()
                .filter(wo -> "READY_FOR_PICKING".equalsIgnoreCase(wo.getWarehouseStatus()) || "PICKING".equalsIgnoreCase(wo.getWarehouseStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseOrderResponse> getPackingOrders() {
        return getWarehouseOrders("ALL").stream()
                .filter(wo -> "PICKED".equalsIgnoreCase(wo.getWarehouseStatus())
                        || "READY_FOR_PACKING".equalsIgnoreCase(wo.getWarehouseStatus())
                        || "PACKING".equalsIgnoreCase(wo.getWarehouseStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseOrderResponse> getReadyToShipOrders() {
        return getWarehouseOrders("ALL").stream()
                .filter(wo -> "PACKED".equalsIgnoreCase(wo.getWarehouseStatus())
                        || "READY_TO_SHIP".equalsIgnoreCase(wo.getWarehouseStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseOrderResponse getWarehouseOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));
        return mapToWarehouseOrderResponse(order);
    }

    @Override
    @Transactional
    public WarehouseOrderResponse startPicking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot pick order in status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        Shipment shipment = getOrCreateShipment(order);
        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING, "Warehouse Picking", "Started picking items for order #" + order.getOrderNumber());

        return mapToWarehouseOrderResponse(order);
    }

    @Override
    @Transactional
    public WarehouseOrderResponse markPicked(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot mark picked for order in status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        Shipment shipment = getOrCreateShipment(order);
        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING, "Warehouse Pick Station", "All " + order.getItems().size() + " items picked and verified against SKU inventory for order #" + order.getOrderNumber());

        return mapToWarehouseOrderResponse(order);
    }

    @Override
    @Transactional
    public WarehouseOrderResponse startPacking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot pack order in status: " + order.getOrderStatus());
        }

        Shipment shipment = getOrCreateShipment(order);
        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING, "Packing Station", "Packing started for order #" + order.getOrderNumber());

        return mapToWarehouseOrderResponse(order);
    }

    @Override
    @Transactional
    public WarehouseOrderResponse markPacked(Long orderId, WarehousePackingRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot mark packed for order in status: " + order.getOrderStatus());
        }

        Shipment shipment = getOrCreateShipment(order);
        String details = "Order packed and sealed.";
        if (request != null) {
            if (request.getPackageWeight() != null && !request.getPackageWeight().isBlank()) {
                details += " Weight: " + request.getPackageWeight() + ".";
            }
            if (request.getPackageDimensions() != null && !request.getPackageDimensions().isBlank()) {
                details += " Dimensions: " + request.getPackageDimensions() + ".";
            }
            if (request.getPackingNotes() != null && !request.getPackingNotes().isBlank()) {
                details += " Notes: " + request.getPackingNotes();
            }
        }

        appendTrackingEvent(shipment, ShipmentStatus.PROCESSING, "Packing Station", details);

        return mapToWarehouseOrderResponse(order);
    }

    @Override
    @Transactional
    public WarehouseOrderResponse readyToShip(Long orderId, WarehouseReadyToShipRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot mark ready to ship for order in status: " + order.getOrderStatus());
        }

        Shipment shipment = getOrCreateShipment(order);

        if (request != null && request.getCarrier() != null && !request.getCarrier().isBlank()) {
            shipment.setCarrier(request.getCarrier().trim());
        }
        if (request != null && request.getEstimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }

        shipment.setStatus(ShipmentStatus.READY_TO_SHIP);

        String desc = "Order is packed, labeled, and staged in Dispatch Bay (Carrier: " + shipment.getCarrier() + ")";
        if (request != null && request.getShippingNotes() != null && !request.getShippingNotes().isBlank()) {
            desc += " - " + request.getShippingNotes();
        }

        appendTrackingEvent(shipment, ShipmentStatus.READY_TO_SHIP, "Dispatch Staging Bay", desc);
        shipmentRepository.save(shipment);

        // Notify customer
        if (order.getUser() != null) {
            notificationService.send(
                    order.getUser(),
                    NotificationType.ORDER_PROCESSING,
                    "Shipment Ready for Pickup",
                    "Your order #" + order.getOrderNumber() + " is packed and ready for carrier pickup with " + shipment.getCarrier() + ". Tracking #: " + shipment.getTrackingNumber(),
                    order.getId()
            );
        }

        return mapToWarehouseOrderResponse(order);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Shipment getOrCreateShipment(Order order) {
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
                .location("Central Fulfillment Center")
                .description("Order received in warehouse for fulfillment")
                .build();
        shipment.getTrackingEvents().add(initialEvent);

        return shipmentRepository.save(shipment);
    }

    private void appendTrackingEvent(Shipment shipment, ShipmentStatus status, String location, String description) {
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .description(description)
                .build();
        shipment.getTrackingEvents().add(event);
        shipmentRepository.save(shipment);
    }

    private WarehouseOrderResponse mapToWarehouseOrderResponse(Order order) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(order.getId());
        Shipment shipment = shipmentOpt.orElse(null);

        List<WarehouseOrderItemResponse> itemResponses = new ArrayList<>();
        int totalItemsCount = 0;

        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                totalItemsCount += oi.getQuantity();
                Long prodId = oi.getProduct() != null ? oi.getProduct().getId() : null;
                String sku = oi.getProduct() != null ? oi.getProduct().getSku() : "N/A";
                String name = oi.getProductNameSnapshot() != null ? oi.getProductNameSnapshot()
                        : (oi.getProduct() != null ? oi.getProduct().getName() : "Product #" + prodId);
                String img = oi.getProductImageUrlSnapshot() != null ? oi.getProductImageUrlSnapshot()
                        : (oi.getProduct() != null ? oi.getProduct().getImageUrl() : null);

                int availableStock = 0;
                int totalStock = 0;
                if (prodId != null) {
                    Optional<Inventory> inv = inventoryRepository.findByProductId(prodId);
                    if (inv.isPresent()) {
                        availableStock = inv.get().getAvailableStock();
                        totalStock = inv.get().getTotalStock();
                    } else if (oi.getProduct() != null) {
                        availableStock = oi.getProduct().getStockQuantity();
                        totalStock = oi.getProduct().getStockQuantity();
                    }
                }

                String pickStatus = "PENDING";
                if (shipment != null && shipment.getTrackingEvents() != null) {
                    boolean hasPickedEvent = shipment.getTrackingEvents().stream()
                            .anyMatch(te -> te.getDescription() != null &&
                                    (te.getDescription().toLowerCase().contains("picked") ||
                                     te.getDescription().toLowerCase().contains("packed") ||
                                     te.getStatus() == ShipmentStatus.READY_TO_SHIP ||
                                     te.getStatus() == ShipmentStatus.SHIPPED));
                    if (hasPickedEvent) {
                        pickStatus = "PICKED";
                    }
                }

                itemResponses.add(WarehouseOrderItemResponse.builder()
                        .id(oi.getId())
                        .productId(prodId)
                        .productName(name)
                        .productSku(sku)
                        .productImageUrl(img)
                        .quantity(oi.getQuantity()) // Exact purchased quantity preserved
                        .unitPrice(oi.getPrice())
                        .vendorProfileId(oi.getVendorProfile() != null ? oi.getVendorProfile().getId() : null)
                        .vendorStoreName(oi.getVendorProfile() != null ? oi.getVendorProfile().getStoreName() : "Store")
                        .availableStock(availableStock)
                        .totalStock(totalStock)
                        .inStock(availableStock >= oi.getQuantity())
                        .pickingStatus(pickStatus)
                        .build());
            }
        }

        String customerName = order.getUser() != null
                ? (order.getUser().getFirstName() != null ? order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "") : order.getUser().getEmail())
                : "Customer";

        return WarehouseOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getUser() != null ? order.getUser().getId() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .customerFullName(customerName.trim())
                .customerPhone(order.getUser() != null ? order.getUser().getPhoneNumber() : null)
                .shippingAddress(order.getShippingAddress())
                .orderStatus(order.getOrderStatus().name())
                .warehouseStatus(deriveWarehouseStatus(order))
                .totalAmount(order.getTotalAmount())
                .totalItems(totalItemsCount)
                .items(itemResponses)
                .shipmentId(shipment != null ? shipment.getId() : null)
                .trackingNumber(shipment != null ? shipment.getTrackingNumber() : null)
                .carrier(shipment != null ? shipment.getCarrier() : null)
                .shipmentStatus(shipment != null ? shipment.getStatus().name() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String deriveWarehouseStatus(Order order) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED) return "CANCELLED";
        if (order.getOrderStatus() == OrderStatus.DELIVERED) return "DELIVERED";
        if (order.getOrderStatus() == OrderStatus.SHIPPED) return "SHIPPED";

        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(order.getId());
        if (shipmentOpt.isPresent()) {
            Shipment s = shipmentOpt.get();
            if (s.getStatus() == ShipmentStatus.READY_TO_SHIP) return "READY_TO_SHIP";
            if (s.getStatus() == ShipmentStatus.SHIPPED) return "SHIPPED";
            if (s.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY) return "OUT_FOR_DELIVERY";
            if (s.getStatus() == ShipmentStatus.DELIVERED) return "DELIVERED";

            if (s.getTrackingEvents() != null && !s.getTrackingEvents().isEmpty()) {
                List<TrackingEvent> events = s.getTrackingEvents();
                for (int i = events.size() - 1; i >= 0; i--) {
                    String d = events.get(i).getDescription();
                    if (d != null) {
                        String lower = d.toLowerCase();
                        if (lower.contains("order packed and sealed")) return "PACKED";
                        if (lower.contains("packing started")) return "PACKING";
                        if (lower.contains("all") && lower.contains("picked")) return "PICKED";
                        if (lower.contains("started picking")) return "PICKING";
                    }
                }
            }
        }

        if (order.getOrderStatus() == OrderStatus.PROCESSING || order.getOrderStatus() == OrderStatus.CONFIRMED) {
            return "READY_FOR_PICKING";
        }

        return "WAITING_FOR_PROCESSING";
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
}

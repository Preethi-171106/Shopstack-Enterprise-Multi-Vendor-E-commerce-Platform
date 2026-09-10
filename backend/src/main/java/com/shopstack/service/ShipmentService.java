package com.shopstack.service;

import com.shopstack.dto.shipment.ShipmentCreateRequest;
import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.dto.shipment.ShipmentStatusUpdateRequest;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Shipment;
import com.shopstack.entity.ShipmentStatus;
import com.shopstack.entity.TrackingEvent;
import com.shopstack.entity.User;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.InvalidShipmentStatusTransitionException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.ShipmentAlreadyExistsException;
import com.shopstack.exception.ShipmentNotFoundException;
import com.shopstack.exception.ShipmentOwnershipException;
import com.shopstack.mapper.ShipmentMapper;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ShipmentRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * ShipmentService — Core service managing shipment creation, tracking updates,
 * status transitions, order synchronization, and ownership validation.
 *
 * <p>Status transition state machine:
 * PROCESSING -> READY_TO_SHIP -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
 * Any state before SHIPPED -> CANCELLED (allowed)
 */
@Service
public class ShipmentService {

    // Valid forward-only transition map
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS = Map.of(
            ShipmentStatus.PROCESSING,        Set.of(ShipmentStatus.READY_TO_SHIP, ShipmentStatus.CANCELLED),
            ShipmentStatus.READY_TO_SHIP,     Set.of(ShipmentStatus.SHIPPED, ShipmentStatus.CANCELLED),
            ShipmentStatus.SHIPPED,           Set.of(ShipmentStatus.OUT_FOR_DELIVERY),
            ShipmentStatus.OUT_FOR_DELIVERY,  Set.of(ShipmentStatus.DELIVERED),
            ShipmentStatus.DELIVERED,         Set.of(ShipmentStatus.RETURNED),
            ShipmentStatus.CANCELLED,         Set.of(),
            ShipmentStatus.RETURNED,          Set.of()
    );

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final ShipmentMapper shipmentMapper;
    private final NotificationService notificationService;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            ShipmentMapper shipmentMapper,
            NotificationService notificationService
    ) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.shipmentMapper = shipmentMapper;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // Warehouse Staff / Admin — Shipment Management
    // =========================================================================

    /**
     * Creates a new shipment for an order, appends the initial TrackingEvent (PROCESSING),
     * and synchronizes the order status to PROCESSING.
     */
    @Transactional
    public ShipmentResponse createShipment(ShipmentCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order with ID " + request.orderId() + " not found"
                ));

        if (shipmentRepository.existsByOrderId(request.orderId())) {
            throw new ShipmentAlreadyExistsException(
                    "Shipment already exists for order ID: " + request.orderId()
            );
        }

        String trackingNumber = generateUniqueTrackingNumber();

        TrackingEvent initialEvent = TrackingEvent.builder()
                .status(ShipmentStatus.PROCESSING)
                .location(request.location())
                .description(request.description() != null
                        ? request.description()
                        : "Shipment created and is being processed")
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .order(order)
                .carrier(request.carrier())
                .status(ShipmentStatus.PROCESSING)
                .shippingAddress(request.shippingAddress())
                .estimatedDeliveryDate(request.estimatedDeliveryDate())
                .build();

        initialEvent.setShipment(shipment);
        shipment.getTrackingEvents().add(initialEvent);

        // Sync order status to PROCESSING
        order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        Shipment saved = shipmentRepository.save(shipment);
        return shipmentMapper.toShipmentResponse(saved);
    }

    /**
     * Lists all shipments in the system (warehouse staff / admin).
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll().stream()
                .map(shipmentMapper::toShipmentResponse)
                .toList();
    }

    /**
     * Gets a shipment by ID (warehouse staff / admin).
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment with ID " + id + " not found"
                ));
        return shipmentMapper.toShipmentResponse(shipment);
    }

    /**
     * Updates shipment status with transition validation, appends a TrackingEvent,
     * and synchronizes the associated Order status.
     */
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long id, ShipmentStatusUpdateRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment with ID " + id + " not found"
                ));

        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus next = request.status();

        validateTransition(current, next);

        // Append tracking event
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .status(next)
                .location(request.location())
                .description(request.description())
                .build();
        shipment.getTrackingEvents().add(event);
        shipment.setStatus(next);

        // Update timestamps for milestones
        if (next == ShipmentStatus.SHIPPED) {
            shipment.setShippedDate(java.time.LocalDateTime.now());
        }
        if (next == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredDate(java.time.LocalDateTime.now());
        }

        // Sync order status
        Order order = shipment.getOrder();
        syncOrderStatus(order, next);
        orderRepository.save(order);

        // Send customer notification based on shipment transition
        if (order.getUser() != null && notificationService != null) {
            switch (next) {
                case SHIPPED -> notificationService.send(
                        order.getUser(),
                        NotificationType.ORDER_SHIPPED,
                        "Order Shipped",
                        "Your order #" + order.getOrderNumber() + " has been shipped with " + shipment.getCarrier() + " (Tracking: " + shipment.getTrackingNumber() + ").",
                        order.getId()
                );
                case OUT_FOR_DELIVERY -> notificationService.send(
                        order.getUser(),
                        NotificationType.ORDER_OUT_FOR_DELIVERY,
                        "Out For Delivery",
                        "Your order #" + order.getOrderNumber() + " is out for delivery today!",
                        order.getId()
                );
                case DELIVERED -> notificationService.send(
                        order.getUser(),
                        NotificationType.ORDER_DELIVERED,
                        "Order Delivered",
                        "Your package for order #" + order.getOrderNumber() + " has been delivered successfully.",
                        order.getId()
                );
                default -> {}
            }
        }

        Shipment saved = shipmentRepository.save(shipment);
        return shipmentMapper.toShipmentResponse(saved);
    }

    // =========================================================================
    // Customer — Own Order Shipment Access
    // =========================================================================

    /**
     * Lists all shipments for orders placed by the authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getCustomerShipments() {
        User user = resolveAuthenticatedUser();
        return shipmentRepository.findByOrderUserId(user.getId()).stream()
                .map(shipmentMapper::toShipmentResponse)
                .toList();
    }

    /**
     * Gets shipment for a specific order ID, verifying it belongs to the authenticated customer.
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getCustomerShipmentByOrderId(Long orderId) {
        User user = resolveAuthenticatedUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order with ID " + orderId + " not found"
                ));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ShipmentOwnershipException(
                    "Access denied: you do not own order with ID " + orderId
            );
        }

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "No shipment found for order with ID " + orderId
                ));

        return shipmentMapper.toShipmentResponse(shipment);
    }

    // =========================================================================
    // Vendor — Own Product Shipments
    // =========================================================================

    /**
     * Lists all shipments that contain items sold by the authenticated vendor's profile.
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getVendorShipments() {
        User user = resolveAuthenticatedUser();
        VendorProfile vendorProfile = vendorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new com.shopstack.exception.VendorProfileNotFoundException(
                        "Vendor profile not found for authenticated user"
                ));

        return shipmentRepository.findDistinctByVendorProfileId(vendorProfile.getId()).stream()
                .map(shipmentMapper::toShipmentResponse)
                .toList();
    }

    // =========================================================================
    // Public / Authenticated — Tracking Number Lookup
    // =========================================================================

    /**
     * Returns shipment details by tracking number (accessible by any authenticated user).
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment with tracking number '" + trackingNumber + "' not found"
                ));
        return shipmentMapper.toShipmentResponse(shipment);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Validates whether the requested status transition is permitted.
     */
    private void validateTransition(ShipmentStatus current, ShipmentStatus next) {
        if (current == next) {
            throw new InvalidShipmentStatusTransitionException(
                    "Shipment is already in status '" + current + "'"
            );
        }
        Set<ShipmentStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new InvalidShipmentStatusTransitionException(
                    "Cannot transition shipment from '" + current + "' to '" + next + "'. " +
                            "Allowed transitions: " + allowed
            );
        }
    }

    /**
     * Syncs the parent Order status when a critical shipment milestone is reached.
     */
    private void syncOrderStatus(Order order, ShipmentStatus shipmentStatus) {
        switch (shipmentStatus) {
            case SHIPPED -> order.setOrderStatus(OrderStatus.SHIPPED);
            case DELIVERED -> order.setOrderStatus(OrderStatus.DELIVERED);
            case CANCELLED -> order.setOrderStatus(OrderStatus.CANCELLED);
            case RETURNED -> order.setOrderStatus(OrderStatus.RETURNED);
            default -> { /* no sync needed for intermediate steps */ }
        }
    }

    /**
     * Generates a unique tracking number in the format TRK-YYYYMMDD-XXXXX.
     * Retries up to 5 times if a collision is found.
     */
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
        throw new IllegalStateException("Failed to generate a unique tracking number after 5 attempts");
    }

    /**
     * Resolves the currently authenticated user from SecurityContext.
     */
    private User resolveAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));
    }
}

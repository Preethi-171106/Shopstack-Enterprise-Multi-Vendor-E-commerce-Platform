package com.shopstack.service;

import com.shopstack.dto.inventory.StockReserveRequest;
import com.shopstack.dto.order.OrderCreateRequest;
import com.shopstack.dto.order.OrderResponse;
import com.shopstack.dto.order.OrderStatusUpdateRequest;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.exception.CouponExpiredException;
import com.shopstack.exception.CouponInactiveException;
import com.shopstack.exception.CouponMinimumAmountException;
import com.shopstack.exception.CouponUsageExceededException;
import com.shopstack.exception.InvalidCouponException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.mapper.OrderMapper;
import com.shopstack.repository.CartItemRepository;
import com.shopstack.repository.CouponRepository;
import com.shopstack.repository.CouponUsageRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CommissionService commissionService;
    private final NotificationService notificationService;
    private final WarehouseAllocationService warehouseAllocationService;
    private final OrderMapper orderMapper;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            InventoryService inventoryService,
            InventoryRepository inventoryRepository,
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            CommissionService commissionService,
            NotificationService notificationService,
            WarehouseAllocationService warehouseAllocationService,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.commissionService = commissionService;
        this.notificationService = notificationService;
        this.warehouseAllocationService = warehouseAllocationService;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        User user = resolveAuthenticatedUser();

        List<CartItem> cartItems = cartItemRepository.findByUserIdForUpdate(user.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create order from an empty cart");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        // ── Step 1: Build order shell ──
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .shippingAddress(request.getShippingAddress())
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .build();

        // ── Step 3: Build order items + calculate subtotal ──
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            if (cartItem == null || cartItem.getProduct() == null) {
                throw new IllegalStateException("One or more cart items are invalid");
            }
            if (cartItem.getQuantity() <= 0) {
                throw new IllegalStateException("Cart item quantity must be greater than zero");
            }
            if (!cartItem.getProduct().isActive()) {
                throw new IllegalStateException("One or more products are no longer available");
            }

            int requested = cartItem.getQuantity();
            if (inventoryRepository.existsByProductId(cartItem.getProduct().getId())) {
                inventoryService.reserveStock(
                        cartItem.getProduct().getId(),
                        new StockReserveRequest(requested, orderNumber, "Reserved for order " + orderNumber)
                );
            } else if (cartItem.getProduct().getStockQuantity() < requested) {
                throw new IllegalStateException("Insufficient stock for product: " + cartItem.getProduct().getName());
            } else {
                cartItem.getProduct().setStockQuantity(cartItem.getProduct().getStockQuantity() - requested);
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .vendorProfile(cartItem.getProduct().getVendorProfile())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getProduct().getPrice())
                    // Snapshot fields — captured NOW so order history is immutable
                    .productNameSnapshot(cartItem.getProduct().getName())
                    .productImageUrlSnapshot(cartItem.getProduct().getImageUrl())
                    .unitPriceSnapshot(cartItem.getProduct().getPrice())
                    .build();
            order.getItems().add(orderItem);

            BigDecimal lineTotal = cartItem.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }
        order.setSubtotalAmount(subtotal);

        // ── Step 4: Apply coupon discount with server-side validation ──
        Coupon appliedCoupon = resolveCoupon(request.getCouponCode(), user, subtotal);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (appliedCoupon != null) {
            BigDecimal eligibleSubtotal = calculateEligibleSubtotal(appliedCoupon, order.getItems());
            if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidCouponException("This coupon is not applicable to the products in your cart.");
            }
            discountAmount = calculateDiscount(appliedCoupon, eligibleSubtotal);
            order.setCouponCode(appliedCoupon.getCode());
        }
        order.setDiscountAmount(discountAmount);

        // ── Step 5: Final total ──
        BigDecimal total = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        order.setTotalAmount(total);

        // ── Step 6: Save order ──
        Order savedOrder = orderRepository.save(order);

        // ── Step 6.3: Multi-Warehouse Allocation & Stock Reservation ──
        if (warehouseAllocationService != null) {
            try {
                warehouseAllocationService.allocateOrderItems(savedOrder);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(OrderService.class)
                        .warn("[OrderService] Warehouse allocation warning for order {}: {}",
                              savedOrder.getOrderNumber(), e.getMessage());
            }
        }

        // ── Step 6.5: Create commission records for each order item ──
        savedOrder.getItems().forEach(item -> {
            try {
                commissionService.createCommission(item);
            } catch (Exception e) {
                // Commission creation is non-critical — log and continue
                org.slf4j.LoggerFactory.getLogger(OrderService.class)
                        .warn("[OrderService] Commission creation failed for item id={}: {}",
                              item.getId(), e.getMessage());
            }
        });

        // ── Step 7: Map to response BEFORE clearing cart
        //    (must happen before @Modifying deleteByUserId clears the persistence context)
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);

        // ── Step 8: Record coupon usage ──
        if (appliedCoupon != null && order.getCouponCode() != null) {
            appliedCoupon.setUsedCount(
                    appliedCoupon.getUsedCount() == null ? 1 : appliedCoupon.getUsedCount() + 1
            );
            couponRepository.save(appliedCoupon);

            com.shopstack.entity.CouponUsage usage = com.shopstack.entity.CouponUsage.builder()
                    .coupon(appliedCoupon)
                    .user(user)
                    .order(savedOrder)
                    .discountAmount(savedOrder.getDiscountAmount() != null ? savedOrder.getDiscountAmount() : BigDecimal.ZERO)
                    .build();
            couponUsageRepository.save(usage);
        }

        // ── Step 9: Clear cart (AFTER mapping — @Modifying clears persistence context) ──
        cartItemRepository.deleteByUserId(user.getId());

        // ── Step 10: Send order-placed notification to customer, vendor(s), and warehouse ──
        notificationService.send(
            user,
            com.shopstack.entity.NotificationType.ORDER_PLACED,
            "Order Placed Successfully",
            "Your order #" + savedOrder.getOrderNumber() + " has been placed. Total: ₹" + savedOrder.getTotalAmount(),
            savedOrder.getId()
        );

        // Notify vendors with products in this order
        java.util.Set<Long> notifiedVendorIds = new java.util.HashSet<>();
        for (OrderItem item : savedOrder.getItems()) {
            if (item.getVendorProfile() != null && item.getVendorProfile().getUser() != null) {
                User vendorUser = item.getVendorProfile().getUser();
                if (notifiedVendorIds.add(vendorUser.getId())) {
                    notificationService.send(
                        vendorUser,
                        com.shopstack.entity.NotificationType.ORDER_PLACED,
                        "New Order Received",
                        "You received a new order for " + item.getProductNameSnapshot() + " (Order #" + savedOrder.getOrderNumber() + ")",
                        savedOrder.getId()
                    );
                }
            }
        }

        // Notify warehouse staff
        notificationService.sendToRole(
            UserRole.WAREHOUSE_STAFF,
            com.shopstack.entity.NotificationType.ORDER_PLACED,
            "New Order Awaiting Fulfillment",
            "Order #" + savedOrder.getOrderNumber() + " is confirmed and awaiting fulfillment.",
            savedOrder.getId()
        );

        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        User user = resolveAuthenticatedUser();
        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        User user = resolveAuthenticatedUser();
        return orderRepository.findByUserId(user.getId())
                .stream().map(orderMapper::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersFiltered(OrderStatus status) {
        if (status == null) {
            return getAllOrders();
        }
        return orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() == status)
                .map(orderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        validateOrderStatusTransition(order.getOrderStatus(), request.getStatus());
        order.setOrderStatus(request.getStatus());
        Order updated = orderRepository.save(order);

        // Notify the customer of status changes
        com.shopstack.entity.NotificationType nType = statusToNotificationType(request.getStatus());
        if (nType != null && updated.getUser() != null) {
            notificationService.send(
                updated.getUser(),
                nType,
                "Order " + request.getStatus().name().replace("_", " "),
                "Your order #" + updated.getOrderNumber() + " is now " + request.getStatus().name().replace("_", " ") + ".",
                updated.getId()
            );
        }

        return orderMapper.toOrderResponse(updated);
    }

    private com.shopstack.entity.NotificationType statusToNotificationType(OrderStatus status) {
        return switch (status) {
            case CONFIRMED        -> com.shopstack.entity.NotificationType.ORDER_CONFIRMED;
            case PROCESSING       -> com.shopstack.entity.NotificationType.ORDER_PROCESSING;
            case SHIPPED          -> com.shopstack.entity.NotificationType.ORDER_SHIPPED;
            case DELIVERED        -> com.shopstack.entity.NotificationType.ORDER_DELIVERED;
            case CANCELLED        -> com.shopstack.entity.NotificationType.ORDER_CANCELLED;
            default               -> null;
        };
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    /**
     * Resolves and validates a coupon for the current customer and order subtotal.
     * Invalid, expired, disabled, over-limit, or inapplicable coupons are rejected.
     */
    private Coupon resolveCoupon(String code, User user, BigDecimal subtotal) {
       if (code == null || code.isBlank()) {
           return null;
       }

       Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
               .orElseThrow(() -> new InvalidCouponException("Coupon not found with code " + code));

       LocalDateTime now = LocalDateTime.now();
       if (Boolean.FALSE.equals(coupon.getActive())) {
           throw new CouponInactiveException("Coupon is disabled.");
       }
       if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
           throw new CouponInactiveException("Coupon is not yet active.");
       }
       if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
           throw new CouponExpiredException("Coupon has expired.");
       }
       if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
           throw new CouponUsageExceededException("Coupon usage limit has been reached.");
       }
       if (coupon.getMinimumOrderAmount() != null && subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
           throw new CouponMinimumAmountException("Order total is less than the minimum required amount for this coupon.");
       }
       if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
           throw new InvalidCouponException("Coupon discount value is invalid.");
       }
       if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE
               && coupon.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
           throw new InvalidCouponException("Coupon discount percentage cannot exceed 100%.");
       }
       if (coupon.getPerUserLimit() != null) {
           int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
           if (userUsage >= coupon.getPerUserLimit()) {
               throw new CouponUsageExceededException("You have reached the maximum usage limit for this coupon.");
           }
       }

       return coupon;
    }

    private BigDecimal calculateEligibleSubtotal(Coupon coupon, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        com.shopstack.entity.CouponApplicabilityScope scope = coupon.getApplicabilityScope() != null
                ? coupon.getApplicabilityScope()
                : com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM;

        BigDecimal eligibleSubtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item == null || item.getProduct() == null) continue;
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            boolean isEligible;
            if (scope.isPlatform()) {
                isEligible = true;
            } else if (scope.isCategory()) {
                isEligible = item.getProduct().getCategory() != null && coupon.getEligibleCategories() != null
                        && coupon.getEligibleCategories().stream().anyMatch(c -> c.getId().equals(item.getProduct().getCategory().getId()));
            } else if (scope.isProduct()) {
                isEligible = coupon.getEligibleProducts() != null
                        && coupon.getEligibleProducts().stream().anyMatch(p -> p.getId().equals(item.getProduct().getId()));
            } else if (scope.isVendor()) {
                isEligible = item.getVendorProfile() != null && coupon.getEligibleVendors() != null
                        && coupon.getEligibleVendors().stream().anyMatch(v -> v.getId().equals(item.getVendorProfile().getId()));
            } else {
                isEligible = true;
            }

            if (isEligible) {
                eligibleSubtotal = eligibleSubtotal.add(lineTotal);
            }
        }
        return eligibleSubtotal;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
                discount = coupon.getMaximumDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        return discount.min(subtotal);
    }

    private void validateOrderStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) throw new IllegalStateException("Order is already in status: " + current);
        boolean valid = switch (current) {
            case PENDING        -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED      -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING     -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED        -> next == OrderStatus.DELIVERED;
            case DELIVERED      -> next == OrderStatus.RETURN_REQUESTED;
            case RETURN_REQUESTED -> next == OrderStatus.RETURNED || next == OrderStatus.CANCELLED;
            case RETURNED       -> next == OrderStatus.REFUNDED;
            default             -> false;
        };
        if (!valid) throw new IllegalStateException("Invalid transition: " + current + " → " + next);
    }

    private User resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("No authenticated user");
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + auth.getName()));
    }
}

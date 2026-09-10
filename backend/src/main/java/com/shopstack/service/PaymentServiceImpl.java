package com.shopstack.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.shopstack.dto.payment.PaymentCreateRequest;
import com.shopstack.dto.payment.PaymentRefundRequest;
import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.dto.payment.PaymentVerifyRequest;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Payment;
import com.shopstack.entity.PaymentGateway;
import com.shopstack.entity.PaymentStatus;
import com.shopstack.entity.User;
import com.shopstack.exception.DuplicatePaymentException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.PaymentNotFoundException;
import com.shopstack.exception.PaymentVerificationException;
import com.shopstack.exception.RefundNotAllowedException;
import com.shopstack.exception.RazorpayNotConfiguredException;
import com.shopstack.mapper.PaymentMapper;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.PaymentRepository;
import com.shopstack.repository.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * PaymentServiceImpl — Implements payment lifecycle: create, verify, view, and refund.
 *
 * <p>Integrates with Razorpay via the official Java SDK. Uses HMAC-SHA256
 * signature verification to confirm authenticity of payment callbacks.</p>
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final RazorpayClient razorpayClient;
    private final RefundService refundService;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            PaymentMapper paymentMapper,
            RazorpayClient razorpayClient,
            RefundService refundService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentMapper = paymentMapper;
        this.razorpayClient = razorpayClient;
        this.refundService = refundService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId, PaymentCreateRequest request) {
        // 1. Verify order exists
        com.shopstack.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // 2. Ensure the authenticated customer owns this order
        User currentUser = resolveAuthenticatedUser();
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to pay for this order");
        }

        // 3. Prevent duplicate payment if already SUCCESS or REFUNDED; allow retry if CREATED, FAILED, or CANCELLED
        java.util.Optional<Payment> existingOpt = paymentRepository.findByOrderId(orderId);
        Payment payment = null;
        if (existingOpt.isPresent()) {
            Payment existingPayment = existingOpt.get();
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS || existingPayment.getStatus() == PaymentStatus.REFUNDED) {
                throw new DuplicatePaymentException("A payment already exists for order ID: " + orderId);
            }
            payment = existingPayment;
        }

        // 4. Determine currency
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().toUpperCase()
                : "INR";

        // Razorpay expects amount in smallest currency unit (paise for INR)
        long amountInPaise = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // 5. Ensure Razorpay is configured with real non-placeholder credentials
        String cleanKeyId = razorpayKeyId != null ? razorpayKeyId.trim().replace("\"", "").replace("'", "") : "";
        String cleanKeySecret = razorpayKeySecret != null ? razorpayKeySecret.trim().replace("\"", "").replace("'", "") : "";

        if (cleanKeyId.isEmpty() || cleanKeySecret.isEmpty()
                || "rzp_test_placeholder".equalsIgnoreCase(cleanKeyId)
                || "rzp_test_mockkeyid".equalsIgnoreCase(cleanKeyId)
                || "mocksecret123456789".equalsIgnoreCase(cleanKeySecret)
                || "placeholder_secret".equalsIgnoreCase(cleanKeySecret)
                || "your_razorpay_key_secret_here".equalsIgnoreCase(cleanKeySecret)) {
            log.warn("Attempt to create Razorpay order when Razorpay keys are not configured or are placeholder values");
            throw new RazorpayNotConfiguredException(
                    "Razorpay payment is not configured. Please set the RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables."
            );
        }

        // 6. Create Razorpay Order
        JSONObject razorpayOrderRequest = new JSONObject();
        razorpayOrderRequest.put("amount", amountInPaise);
        razorpayOrderRequest.put("currency", currency);
        razorpayOrderRequest.put("receipt", order.getOrderNumber());

        String gatewayOrderId;
        try {
            Order razorpayOrder = razorpayClient.orders.create(razorpayOrderRequest);
            gatewayOrderId = razorpayOrder.get("id");
            log.info("Razorpay order created: {}", gatewayOrderId);
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", orderId, e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errorMsg.toLowerCase().contains("authentication failed") || errorMsg.toLowerCase().contains("bad_request_error")) {
                throw new RazorpayNotConfiguredException(
                        "Razorpay authentication failed: Invalid or expired Razorpay Key ID and Secret. "
                        + "Please verify your API keys in the Razorpay Dashboard (Settings > API Keys) "
                        + "and set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables."
                );
            }
            throw new PaymentVerificationException("Failed to create Razorpay order: " + errorMsg);
        }

        // 7. Save or update payment with CREATED status
        if (payment == null) {
            String internalPaymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            payment = Payment.builder()
                    .order(order)
                    .paymentId(internalPaymentId)
                    .gatewayOrderId(gatewayOrderId)
                    .amount(order.getTotalAmount())
                    .currency(currency)
                    .paymentMethod(request.getPaymentMethod())
                    .gateway(PaymentGateway.RAZORPAY)
                    .status(PaymentStatus.CREATED)
                    .build();
        } else {
            payment.setGatewayOrderId(gatewayOrderId);
            payment.setAmount(order.getTotalAmount());
            payment.setCurrency(currency);
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setStatus(PaymentStatus.CREATED);
            payment.setFailureReason(null);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment record saved: {} for order: {}", saved.getPaymentId(), orderId);

        // Map to response and expose only safe fields to client
        com.shopstack.dto.payment.PaymentResponse response = paymentMapper.toPaymentResponse(saved);
        response.setRazorpayKeyId(cleanKeyId);
        response.setAmountInPaise(amountInPaise);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        // 1. Retrieve payment by order ID
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order ID: " + request.getOrderId()));

        // 2. Ensure the authenticated customer owns this payment
        User currentUser = resolveAuthenticatedUser();
        if (!payment.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to verify this payment");
        }

        // Return immediately if payment is already marked SUCCESS
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return paymentMapper.toPaymentResponse(payment);
        }

        // 3. Verify Razorpay HMAC-SHA256 signature
        String cleanSecret = razorpayKeySecret != null ? razorpayKeySecret.trim().replace("\"", "").replace("'", "") : "";
        if (cleanSecret.isEmpty() || "placeholder_secret".equalsIgnoreCase(cleanSecret)
                || "mocksecret123456789".equalsIgnoreCase(cleanSecret)
                || "your_razorpay_key_secret_here".equalsIgnoreCase(cleanSecret)) {
            log.warn("Attempt to verify payment when Razorpay secret is not configured");
            throw new RazorpayNotConfiguredException("Razorpay payment is not configured.");
        }

        boolean signatureValid;
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            signatureValid = Utils.verifyPaymentSignature(attributes, cleanSecret);
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification error: {}", e.getMessage());
            signatureValid = false;
        }

        // 4. Update payment and order status based on signature result
        com.shopstack.entity.Order order = payment.getOrder();
        if (signatureValid) {
            payment.setGatewayTransactionId(request.getRazorpayPaymentId());
            payment.setGatewayOrderId(request.getRazorpayOrderId());
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setOrderStatus(OrderStatus.PROCESSING);
            log.info("Payment verified successfully for order: {}", request.getOrderId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay signature verification failed");
            log.warn("Payment verification failed for order: {}", request.getOrderId());
        }

        orderRepository.save(order);
        Payment updated = paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PAYMENT BY ORDER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order ID: " + orderId));

        User currentUser = resolveAuthenticatedUser();
        if (!payment.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this payment");
        }

        return paymentMapper.toPaymentResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFUND PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long orderId, PaymentRefundRequest request) {
        // 1. Find payment for the order
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order ID: " + orderId));

        // 2. Ensure the authenticated customer owns this payment
        User currentUser = resolveAuthenticatedUser();
        if (!payment.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to refund this payment");
        }

        // 3. Only SUCCESS payments can be refunded
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RefundNotAllowedException(
                    "Refund is only allowed for payments with SUCCESS status. Current status: " + payment.getStatus());
        }

        // 4. Ensure Razorpay is configured
        String cleanRefundSecret = razorpayKeySecret != null ? razorpayKeySecret.trim().replace("\"", "").replace("'", "") : "";
        if (cleanRefundSecret.isEmpty() || "placeholder_secret".equalsIgnoreCase(cleanRefundSecret)) {
            throw new com.shopstack.exception.RazorpayNotConfiguredException("Razorpay payment is not configured.");
        }

        // Determine refund amount (full refund by default if not specified)
        BigDecimal refundAmount = (request != null && request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0)
                ? request.getAmount()
                : payment.getAmount();

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RefundNotAllowedException(
                    "Refund amount (" + refundAmount + ") cannot exceed paid amount (" + payment.getAmount() + ")");
        }

        String reason = request != null ? request.getReason() : null;

        // 5. Process refund via RefundService which calls Razorpay and records Refund entity
        com.shopstack.entity.Refund refund = refundService.processGatewayRefund(payment, refundAmount, reason);

        // 6. Update payment and order depending on refund result
        if (refund.getStatus() == com.shopstack.entity.RefundStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setFailureReason(reason != null ? "Refund reason: " + reason : null);

            com.shopstack.entity.Order order = payment.getOrder();
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            Payment updated = paymentRepository.save(payment);
            log.info("Payment refunded successfully for order: {} (refund id: {})", orderId, refund.getRefundId());
            return paymentMapper.toPaymentResponse(updated);
        } else if (refund.getStatus() == com.shopstack.entity.RefundStatus.PROCESSING) {
            payment.setStatus(PaymentStatus.PENDING);
            Payment updated = paymentRepository.save(payment);
            log.info("Refund is processing for order: {} (refund id: {})", orderId, refund.getRefundId());
            return paymentMapper.toPaymentResponse(updated);
        } else {
            throw new com.shopstack.exception.RefundFailedException("Refund failed for payment: " + payment.getPaymentId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — LIST ALL PAYMENTS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — GET PAYMENT BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + id));
        return paymentMapper.toPaymentResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the currently authenticated user from the Spring Security context.
     */
    private User resolveAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}

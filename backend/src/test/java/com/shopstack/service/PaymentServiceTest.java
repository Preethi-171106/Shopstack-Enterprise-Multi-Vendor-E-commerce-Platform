package com.shopstack.service;

import com.razorpay.RazorpayClient;
import com.shopstack.dto.payment.PaymentCreateRequest;
import com.shopstack.dto.payment.PaymentRefundRequest;
import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.dto.payment.PaymentVerifyRequest;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.Payment;
import com.shopstack.entity.PaymentGateway;
import com.shopstack.entity.PaymentMethod;
import com.shopstack.entity.PaymentStatus;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.exception.DuplicatePaymentException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.PaymentNotFoundException;
import com.shopstack.exception.RefundNotAllowedException;
import com.shopstack.mapper.PaymentMapper;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.PaymentRepository;
import com.shopstack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentServiceTest — Unit tests for {@link PaymentServiceImpl}.
 *
 * <p>Uses Mockito to mock repositories and RazorpayClient; the PaymentMapper
 * is spied on to allow real mapping logic.</p>
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @Mock
    private RefundService refundService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User customerUser;
    private User otherUser;
    private Order sampleOrder;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_keyid123");
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "testsecret123456789");

        customerUser = User.builder()
                .id(1L)
                .email("customer@shopstack.com")
                .role(UserRole.CUSTOMER)
                .build();

        otherUser = User.builder()
                .id(99L)
                .email("other@shopstack.com")
                .role(UserRole.CUSTOMER)
                .build();

        sampleOrder = Order.builder()
                .id(10L)
                .orderNumber("ORD-20240001")
                .user(customerUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .shippingAddress("123 Main Street, City")
                .build();

        samplePayment = Payment.builder()
                .id(100L)
                .order(sampleOrder)
                .paymentId("PAY-ABC123")
                .gatewayOrderId("order_razorpay123")
                .amount(new BigDecimal("999.99"))
                .currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .gateway(PaymentGateway.RAZORPAY)
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void authenticateAs(User user, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE PAYMENT TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createPayment()")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void shouldThrowWhenOrderNotFound() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    paymentService.createPayment(999L, PaymentCreateRequest.builder()
                            .paymentMethod(PaymentMethod.UPI).build()))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("999");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicatePaymentException when successful payment already exists for order")
        void shouldThrowWhenDuplicatePayment() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            samplePayment.setStatus(PaymentStatus.SUCCESS);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));

            assertThatThrownBy(() ->
                    paymentService.createPayment(10L, PaymentCreateRequest.builder()
                            .paymentMethod(PaymentMethod.UPI).build()))
                    .isInstanceOf(DuplicatePaymentException.class)
                    .hasMessageContaining("10");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user does not own the order")
        void shouldThrowWhenCustomerDoesNotOwnOrder() {
            authenticateAs(otherUser, "ROLE_CUSTOMER");
            when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findByEmailIgnoreCase("other@shopstack.com"))
                    .thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() ->
                    paymentService.createPayment(10L, PaymentCreateRequest.builder()
                            .paymentMethod(PaymentMethod.UPI).build()))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create demo payment with SUCCESS status and update order to PROCESSING when Razorpay keys are missing")
        void shouldCreateDemoPaymentWhenRazorpayNotConfigured() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "");
            org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "");
            when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(105L);
                return p;
            });
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.createPayment(10L, PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI).build());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.getGatewayOrderId()).startsWith("order_demo_");
            assertThat(response.getRazorpayKeyId()).isEqualTo("demo_mode");
            assertThat(sampleOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(paymentRepository).save(any(Payment.class));
            verify(orderRepository).save(sampleOrder);
        }

        @Test
        @DisplayName("Should create payment for Card method when Razorpay is configured")
        void shouldCreatePaymentForCardMethod() throws Exception {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

            // Mock razorpay order creation
            org.json.JSONObject mockOrderJson = new org.json.JSONObject();
            mockOrderJson.put("id", "order_rzp_card_123");
            com.razorpay.Order mockRzpOrder = new com.razorpay.Order(mockOrderJson);

            com.razorpay.OrderClient mockOrderClient = org.mockito.Mockito.mock(com.razorpay.OrderClient.class);
            when(mockOrderClient.create(any(org.json.JSONObject.class))).thenReturn(mockRzpOrder);
            org.springframework.test.util.ReflectionTestUtils.setField(razorpayClient, "orders", mockOrderClient);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(101L);
                return p;
            });

            PaymentResponse response = paymentService.createPayment(10L, PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.CARD)
                    .currency("INR")
                    .build());

            assertThat(response).isNotNull();
            assertThat(response.getGatewayOrderId()).isEqualTo("order_rzp_card_123");
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_keyid123");
            assertThat(response.getAmountInPaise()).isEqualTo(99999L);
        }

        @Test
        @DisplayName("Should create payment for UPI method and allow retry on FAILED payment without duplicate order")
        void shouldAllowPaymentRetryOnFailedPayment() throws Exception {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            samplePayment.setStatus(PaymentStatus.FAILED);

            when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));

            org.json.JSONObject mockOrderJson = new org.json.JSONObject();
            mockOrderJson.put("id", "order_rzp_retry_456");
            com.razorpay.Order mockRzpOrder = new com.razorpay.Order(mockOrderJson);

            com.razorpay.OrderClient mockOrderClient = org.mockito.Mockito.mock(com.razorpay.OrderClient.class);
            when(mockOrderClient.create(any(org.json.JSONObject.class))).thenReturn(mockRzpOrder);
            org.springframework.test.util.ReflectionTestUtils.setField(razorpayClient, "orders", mockOrderClient);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.createPayment(10L, PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build());

            assertThat(response).isNotNull();
            assertThat(response.getGatewayOrderId()).isEqualTo("order_rzp_retry_456");
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CREATED);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PAYMENT BY ORDER TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPaymentByOrder()")
    class GetPaymentByOrderTests {

        @Test
        @DisplayName("Should return payment response when found and owned by customer")
        void shouldReturnPaymentWhenOwnedByCustomer() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));

            PaymentResponse response = paymentService.getPaymentByOrder(10L);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(10L);
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CREATED);
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when no payment for order")
        void shouldThrowWhenPaymentNotFound() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByOrder(999L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when different customer tries to access")
        void shouldThrowWhenCustomerDoesNotOwnPayment() {
            authenticateAs(otherUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("other@shopstack.com"))
                    .thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> paymentService.getPaymentByOrder(10L))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyPayment()")
    class VerifyPaymentTests {

        @Test
        @DisplayName("Should mark payment as SUCCESS when signature is valid")
        void shouldMarkSuccessOnValidSignature() throws Exception {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            String validSignature = com.razorpay.Utils.getHash("order_razorpay123|pay_razorpay456", "testsecret123456789");

            PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                    .orderId(10L)
                    .razorpayOrderId("order_razorpay123")
                    .razorpayPaymentId("pay_razorpay456")
                    .razorpaySignature(validSignature)
                    .build();

            PaymentResponse response = paymentService.verifyPayment(request);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.getGatewayTransactionId()).isEqualTo("pay_razorpay456");
            assertThat(sampleOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(paymentRepository).save(samplePayment);
            verify(orderRepository).save(sampleOrder);
        }

        @Test
        @DisplayName("Should mark payment as FAILED when signature is invalid")
        void shouldMarkFailedOnInvalidSignature() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                    .orderId(10L)
                    .razorpayOrderId("order_razorpay123")
                    .razorpayPaymentId("pay_razorpay456")
                    .razorpaySignature("invalidsignature")
                    .build();

            // Razorpay signature verification will fail (invalid mock credentials)
            PaymentResponse response = paymentService.verifyPayment(request);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(response.getFailureReason()).isNotBlank();
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when no payment exists")
        void shouldThrowWhenPaymentNotFound() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

            PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                    .orderId(999L)
                    .razorpayOrderId("rzp_order_x")
                    .razorpayPaymentId("rzp_pay_y")
                    .razorpaySignature("sig_z")
                    .build();

            assertThatThrownBy(() -> paymentService.verifyPayment(request))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFUND PAYMENT TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refundPayment()")
    class RefundPaymentTests {

        @Test
        @DisplayName("Should refund payment when status is SUCCESS")
        void shouldRefundSuccessfulPayment() {
            samplePayment.setStatus(PaymentStatus.SUCCESS);
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentRefundRequest request = PaymentRefundRequest.builder()
                    .reason("Customer requested cancellation")
                    .build();

            // Mock RefundService to return a successful Refund
            com.shopstack.entity.Refund mockRefund = com.shopstack.entity.Refund.builder()
                    .id(1L)
                    .refundId("rfnd_test_123")
                    .payment(samplePayment)
                    .amount(samplePayment.getAmount())
                    .gateway(samplePayment.getGateway())
                    .status(com.shopstack.entity.RefundStatus.SUCCESS)
                    .transactionId(samplePayment.getGatewayTransactionId())
                    .remarks("Customer requested cancellation")
                    .build();

            when(refundService.processGatewayRefund(any(Payment.class), any(java.math.BigDecimal.class), anyString()))
                    .thenReturn(mockRefund);

            PaymentResponse response = paymentService.refundPayment(10L, request);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(response.getFailureReason()).contains("Customer requested cancellation");
        }

        @Test
        @DisplayName("Should throw RefundNotAllowedException when payment is not in SUCCESS status")
        void shouldThrowWhenRefundingNonSuccessPayment() {
            samplePayment.setStatus(PaymentStatus.CREATED);
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));

            assertThatThrownBy(() ->
                    paymentService.refundPayment(10L, null))
                    .isInstanceOf(RefundNotAllowedException.class)
                    .hasMessageContaining("CREATED");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RefundNotAllowedException when payment is already REFUNDED")
        void shouldThrowWhenAlreadyRefunded() {
            samplePayment.setStatus(PaymentStatus.REFUNDED);
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));

            assertThatThrownBy(() ->
                    paymentService.refundPayment(10L, null))
                    .isInstanceOf(RefundNotAllowedException.class)
                    .hasMessageContaining("REFUNDED");
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when order has no payment")
        void shouldThrowWhenNoPaymentForOrder() {
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    paymentService.refundPayment(10L, null))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw RefundNotAllowedException when refund amount exceeds paid amount")
        void shouldThrowWhenRefundAmountExceedsPaid() {
            samplePayment.setStatus(PaymentStatus.SUCCESS);
            authenticateAs(customerUser, "ROLE_CUSTOMER");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(samplePayment));
            when(userRepository.findByEmailIgnoreCase("customer@shopstack.com"))
                    .thenReturn(Optional.of(customerUser));

            PaymentRefundRequest request = PaymentRefundRequest.builder()
                    .amount(new BigDecimal("5000.00"))
                    .reason("Too much")
                    .build();

            assertThatThrownBy(() -> paymentService.refundPayment(10L, request))
                    .isInstanceOf(RefundNotAllowedException.class)
                    .hasMessageContaining("cannot exceed paid amount");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Admin payment operations")
    class AdminPaymentTests {

        @Test
        @DisplayName("getAllPayments() should return all payments")
        void shouldReturnAllPayments() {
            when(paymentRepository.findAllByOrderByCreatedAtDesc())
                    .thenReturn(List.of(samplePayment));

            List<PaymentResponse> result = paymentService.getAllPayments();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getPaymentById() should return payment by ID")
        void shouldReturnPaymentById() {
            when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

            PaymentResponse result = paymentService.getPaymentById(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("getPaymentById() should throw PaymentNotFoundException for unknown ID")
        void shouldThrowWhenPaymentIdNotFound() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}

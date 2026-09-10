package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.payment.PaymentCreateRequest;
import com.shopstack.dto.payment.PaymentRefundRequest;
import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.dto.payment.PaymentVerifyRequest;
import com.shopstack.entity.PaymentGateway;
import com.shopstack.entity.PaymentMethod;
import com.shopstack.entity.PaymentStatus;
import com.shopstack.exception.DuplicatePaymentException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.exception.PaymentNotFoundException;
import com.shopstack.exception.RefundNotAllowedException;
import com.shopstack.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PaymentControllerTest — MockMvc integration tests for {@link PaymentController}
 * and {@link AdminPaymentController}.
 *
 * <p>Uses SpringBootTest with MockMvc. PaymentService is mocked to isolate
 * controller-layer behaviour including RBAC enforcement.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentResponse samplePaymentResponse;

    @BeforeEach
    void setUp() {
        samplePaymentResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(10L)
                .orderNumber("ORD-20240001")
                .paymentId("PAY-ABC123")
                .gatewayOrderId("order_rzp_abc123")
                .amount(new BigDecimal("999.99"))
                .currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .gateway(PaymentGateway.RAZORPAY)
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/payments/create/{orderId}")
    class CreatePaymentTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should create payment with 201 for CUSTOMER role")
        void shouldCreatePaymentForCustomer() throws Exception {
            given(paymentService.createPayment(eq(10L), any(PaymentCreateRequest.class)))
                    .willReturn(samplePaymentResponse);

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            mockMvc.perform(post("/api/payments/create/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("PAY-ABC123"))
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.orderId").value(10));
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("Should return 403 for VENDOR role")
        void shouldForbidVendorFromCreatingPayment() throws Exception {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            mockMvc.perform(post("/api/payments/create/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Should return 403 for WAREHOUSE_STAFF role")
        void shouldForbidWarehouseFromCreatingPayment() throws Exception {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            mockMvc.perform(post("/api/payments/create/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated access")
        void shouldReturn401ForUnauthenticated() throws Exception {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            mockMvc.perform(post("/api/payments/create/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 409 when duplicate payment exists")
        void shouldReturn409OnDuplicatePayment() throws Exception {
            given(paymentService.createPayment(eq(10L), any(PaymentCreateRequest.class)))
                    .willThrow(new DuplicatePaymentException("A payment already exists for order ID: 10"));

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            mockMvc.perform(post("/api/payments/create/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            given(paymentService.createPayment(eq(999L), any(PaymentCreateRequest.class)))
                    .willThrow(new OrderNotFoundException("Order not found with ID: 999"));

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .paymentMethod(PaymentMethod.CARD)
                    .build();

            mockMvc.perform(post("/api/payments/create/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/payments/verify")
    class VerifyPaymentTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should verify payment and return 200")
        void shouldVerifyPaymentSuccessfully() throws Exception {
            PaymentResponse verifiedResponse = PaymentResponse.builder()
                    .id(1L)
                    .orderId(10L)
                    .orderNumber("ORD-20240001")
                    .paymentId("PAY-ABC123")
                    .status(PaymentStatus.SUCCESS)
                    .amount(new BigDecimal("999.99"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .gateway(PaymentGateway.RAZORPAY)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(paymentService.verifyPayment(any(PaymentVerifyRequest.class)))
                    .willReturn(verifiedResponse);

            PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                    .orderId(10L)
                    .razorpayOrderId("order_razorpay123")
                    .razorpayPaymentId("pay_razorpay456")
                    .razorpaySignature("sig_valid_abc")
                    .build();

            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 403 for ADMIN role trying to verify payment")
        void shouldForbidAdminFromVerifyingPayment() throws Exception {
            PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                    .orderId(10L)
                    .razorpayOrderId("order_razorpay123")
                    .razorpayPaymentId("pay_razorpay456")
                    .razorpaySignature("sig_abc")
                    .build();

            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PAYMENT BY ORDER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/payments/order/{orderId}")
    class GetPaymentByOrderTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return payment for CUSTOMER role")
        void shouldReturnPaymentForCustomer() throws Exception {
            given(paymentService.getPaymentByOrder(10L)).willReturn(samplePaymentResponse);

            mockMvc.perform(get("/api/payments/order/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(10))
                    .andExpect(jsonPath("$.orderNumber").value("ORD-20240001"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 404 when no payment for order")
        void shouldReturn404WhenNoPayment() throws Exception {
            given(paymentService.getPaymentByOrder(999L))
                    .willThrow(new PaymentNotFoundException("No payment found for order ID: 999"));

            mockMvc.perform(get("/api/payments/order/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("Should return 403 for VENDOR accessing payment")
        void shouldForbidVendorAccess() throws Exception {
            mockMvc.perform(get("/api/payments/order/10"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFUND PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/payments/refund/{orderId}")
    class RefundPaymentTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should refund payment and return 200 for CUSTOMER role")
        void shouldRefundPaymentForCustomer() throws Exception {
            PaymentResponse refundedResponse = PaymentResponse.builder()
                    .id(1L)
                    .orderId(10L)
                    .orderNumber("ORD-20240001")
                    .paymentId("PAY-ABC123")
                    .status(PaymentStatus.REFUNDED)
                    .amount(new BigDecimal("999.99"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .gateway(PaymentGateway.RAZORPAY)
                    .failureReason("Refund reason: Changed my mind")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(paymentService.refundPayment(eq(10L), any()))
                    .willReturn(refundedResponse);

            PaymentRefundRequest request = PaymentRefundRequest.builder()
                    .reason("Changed my mind")
                    .build();

            mockMvc.perform(post("/api/payments/refund/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 400 when payment is not in SUCCESS status")
        void shouldReturn400WhenRefundNotAllowed() throws Exception {
            given(paymentService.refundPayment(eq(10L), any()))
                    .willThrow(new RefundNotAllowedException(
                            "Refund is only allowed for payments with SUCCESS status. Current status: CREATED"));

            PaymentRefundRequest request = PaymentRefundRequest.builder()
                    .reason("cancel order")
                    .build();

            mockMvc.perform(post("/api/payments/refund/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN PAYMENT ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/admin/payments")
    class AdminPaymentListTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return all payments for ADMIN role")
        void shouldReturnAllPaymentsForAdmin() throws Exception {
            given(paymentService.getAllPayments())
                    .willReturn(java.util.List.of(samplePaymentResponse));

            mockMvc.perform(get("/api/admin/payments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].paymentId").value("PAY-ABC123"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 for CUSTOMER accessing admin payments")
        void shouldForbidCustomerFromAdminEndpoint() throws Exception {
            mockMvc.perform(get("/api/admin/payments"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("Should return 403 for VENDOR accessing admin payments")
        void shouldForbidVendorFromAdminEndpoint() throws Exception {
            mockMvc.perform(get("/api/admin/payments"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/payments/{id}")
    class AdminPaymentByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return payment by ID for ADMIN role")
        void shouldReturnPaymentByIdForAdmin() throws Exception {
            given(paymentService.getPaymentById(1L)).willReturn(samplePaymentResponse);

            mockMvc.perform(get("/api/admin/payments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("CREATED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent payment ID")
        void shouldReturn404ForMissingPayment() throws Exception {
            given(paymentService.getPaymentById(999L))
                    .willThrow(new PaymentNotFoundException("Payment not found with ID: 999"));

            mockMvc.perform(get("/api/admin/payments/999"))
                    .andExpect(status().isNotFound());
        }
    }
}

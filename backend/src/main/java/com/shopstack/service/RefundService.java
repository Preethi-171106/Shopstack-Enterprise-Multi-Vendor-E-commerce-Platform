package com.shopstack.service;

import com.razorpay.RazorpayException;
import com.razorpay.RazorpayClient;
import com.shopstack.dto.returns.RefundResponse;
import com.shopstack.entity.Payment;
import com.shopstack.entity.Refund;
import com.shopstack.entity.RefundStatus;
import com.shopstack.entity.ReturnRequest;
import com.shopstack.exception.RefundFailedException;
import com.shopstack.exception.RazorpayNotConfiguredException;
import com.shopstack.mapper.RefundMapper;
import com.shopstack.repository.RefundRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final RefundMapper refundMapper;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    public RefundService(RefundRepository refundRepository, RefundMapper refundMapper, RazorpayClient razorpayClient) {
        this.refundRepository = refundRepository;
        this.refundMapper = refundMapper;
        this.razorpayClient = razorpayClient;
    }

    @Transactional
    public Refund createRefundRecord(Payment payment, ReturnRequest returnRequest, String remarks) {
        String refundId = "RFND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        Refund refund = Refund.builder()
                .refundId(refundId)
                .payment(payment)
                .returnRequest(returnRequest)
                .amount(returnRequest != null ? returnRequest.getRefundAmount() : payment.getAmount())
                .gateway(payment.getGateway())
                .status(RefundStatus.PROCESSED)
                .transactionId(payment.getGatewayTransactionId()) // using original payment tx for now
                .remarks(remarks)
                .processedAt(LocalDateTime.now())
                .build();

        return refundRepository.save(refund);
    }

    /**
     * Processes a refund through the configured Razorpay gateway and records the refund.
     *
     * @param payment the payment to refund (must have gateway transaction id)
     * @param amount  refund amount in major currency unit (e.g., 999.99)
     * @param reason  optional refund remarks
     * @return the persisted Refund
     */
    @Transactional
    public Refund processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            log.warn("Razorpay secret not configured - cannot process refund");
            throw new RazorpayNotConfiguredException("Razorpay payment is not configured.");
        }

        if (payment.getGatewayTransactionId() == null || payment.getGatewayTransactionId().isBlank()) {
            throw new RefundFailedException("Cannot process refund: Gateway transaction ID is missing for payment " + payment.getPaymentId());
        }

        // Prevent refund if total refunded amount would exceed payment amount
        List<Refund> existing = refundRepository.findByPayment(payment);
        BigDecimal totalAlreadyRefunded = existing.stream()
                .filter(r -> r.getStatus() == RefundStatus.SUCCESS || r.getStatus() == RefundStatus.PROCESSING)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAlreadyRefunded.add(amount).compareTo(payment.getAmount()) > 0) {
            throw new RefundFailedException("Refund amount (" + amount + ") exceeds remaining refundable amount ("
                    + payment.getAmount().subtract(totalAlreadyRefunded) + ")");
        }

        // Prepare amount in paise
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();

        JSONObject params = new JSONObject();
        params.put("amount", amountInPaise);

        try {
            com.razorpay.Refund razorpayRefund = razorpayClient.payments.refund(payment.getGatewayTransactionId(), params);
            String gatewayRefundId = razorpayRefund.has("id") && razorpayRefund.get("id") != null
                    ? razorpayRefund.get("id").toString()
                    : null;
            String rpStatus = razorpayRefund.has("status") && razorpayRefund.get("status") != null
                    ? razorpayRefund.get("status").toString()
                    : "processed";

            RefundStatus status = RefundStatus.PROCESSING;
            if ("processed".equalsIgnoreCase(rpStatus) || "success".equalsIgnoreCase(rpStatus)) {
                status = RefundStatus.SUCCESS;
            } else if ("failed".equalsIgnoreCase(rpStatus)) {
                status = RefundStatus.FAILED;
            }

            Refund refund = Refund.builder()
                    .refundId(gatewayRefundId != null ? gatewayRefundId : ("RFND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase()))
                    .payment(payment)
                    .returnRequest(null)
                    .amount(amount)
                    .gateway(payment.getGateway())
                    .status(status)
                    .transactionId(payment.getGatewayTransactionId())
                    .remarks(reason)
                    .processedAt(LocalDateTime.now())
                    .build();

            return refundRepository.save(refund);

        } catch (RazorpayException e) {
            log.error("Razorpay refund call failed: {}", e.getMessage());
            throw new RefundFailedException("Razorpay refund failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getAllRefunds() {
        return refundRepository.findAll().stream()
                .map(refundMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long id) {
        return refundRepository.findById(id)
                .map(refundMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
    }
}

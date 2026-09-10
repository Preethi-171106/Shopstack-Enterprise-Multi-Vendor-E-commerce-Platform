package com.shopstack.mapper;

import com.shopstack.dto.returns.RefundResponse;
import com.shopstack.entity.Refund;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        if (refund == null) {
            return null;
        }

        return RefundResponse.builder()
                .id(refund.getId())
                .refundId(refund.getRefundId())
                .paymentId(refund.getPayment().getId())
                .returnRequestId(refund.getReturnRequest().getId())
                .returnNumber(refund.getReturnRequest().getReturnNumber())
                .amount(refund.getAmount())
                .gateway(refund.getGateway())
                .status(refund.getStatus())
                .transactionId(refund.getTransactionId())
                .remarks(refund.getRemarks())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    public List<RefundResponse> toResponseList(List<Refund> refunds) {
        if (refunds == null) {
            return null;
        }
        return refunds.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

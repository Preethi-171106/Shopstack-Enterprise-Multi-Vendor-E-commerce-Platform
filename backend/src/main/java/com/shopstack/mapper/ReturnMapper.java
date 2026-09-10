package com.shopstack.mapper;

import com.shopstack.dto.returns.ReturnResponse;
import com.shopstack.entity.ReturnRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReturnMapper {

    public ReturnResponse toResponse(ReturnRequest request) {
        if (request == null) {
            return null;
        }

        return ReturnResponse.builder()
                .id(request.getId())
                .returnNumber(request.getReturnNumber())
                .orderId(request.getOrder().getId())
                .orderNumber(request.getOrder().getOrderNumber())
                .userId(request.getUser().getId())
                .userEmail(request.getUser().getEmail())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(request.getStatus())
                .refundAmount(request.getRefundAmount())
                .requestedAt(request.getRequestedAt())
                .approvedAt(request.getApprovedAt())
                .rejectedAt(request.getRejectedAt())
                .completedAt(request.getCompletedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public List<ReturnResponse> toResponseList(List<ReturnRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

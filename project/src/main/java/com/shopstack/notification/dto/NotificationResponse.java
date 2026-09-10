package com.shopstack.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        Long id,
        Long userId,
        String title,
        String message,
        String notificationType,
        String channel,
        String status,
        String referenceId,
        String referenceType,
        Instant createdAt,
        Instant readAt
) {
}

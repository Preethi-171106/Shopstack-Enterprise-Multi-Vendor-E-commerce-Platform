package com.shopstack.notification.mapper;

import com.shopstack.notification.entity.Notification;
import com.shopstack.notification.dto.NotificationResponse;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getNotificationType() != null ? notification.getNotificationType().name() : null,
                notification.getChannel() != null ? notification.getChannel().name() : null,
                notification.getStatus() != null ? notification.getStatus().name() : null,
                notification.getReferenceId(),
                notification.getReferenceType() != null ? notification.getReferenceType().name() : null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}

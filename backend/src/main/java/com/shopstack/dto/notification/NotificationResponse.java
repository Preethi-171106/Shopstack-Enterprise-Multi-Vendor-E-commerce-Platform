package com.shopstack.dto.notification;

import com.shopstack.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private boolean readStatus;
    private Long referenceId;
    private LocalDateTime createdAt;
}

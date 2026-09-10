package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification — in-app notification record stored per user.
 *
 * <p>Notifications are created server-side when key events occur (order placed,
 * order status changed, return approved, etc.).  The frontend polls or fetches
 * notifications via GET /api/notifications.
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_id",       columnList = "user_id"),
                @Index(name = "idx_notifications_read_status",   columnList = "read_status"),
                @Index(name = "idx_notifications_created_at",    columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who should see this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Short title shown in the notification bell/list. */
    @Column(nullable = false, length = 200)
    private String title;

    /** Detailed message body. */
    @Column(nullable = false, length = 1000)
    private String message;

    /** Category of notification for filtering/icons. */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /** Whether the user has marked this notification as read. */
    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private boolean readStatus = false;

    /** Optional reference to the related entity (e.g. order ID). */
    @Column(name = "reference_id")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

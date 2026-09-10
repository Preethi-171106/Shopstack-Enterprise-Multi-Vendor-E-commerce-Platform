package com.shopstack.service;

import com.shopstack.dto.notification.NotificationResponse;
import com.shopstack.entity.Notification;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.exception.NotificationNotFoundException;
import com.shopstack.repository.NotificationRepository;
import com.shopstack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationService — creates, retrieves, and manages in-app notifications.
 *
 * <p>Other services call {@link #send(User, NotificationType, String, String, Long)}
 * or {@link #sendToRole(UserRole, NotificationType, String, String, Long)}
 * to create notifications when important events occur.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates and persists a notification for a specific user.
     */
    @Transactional
    public void send(User user, NotificationType type, String title, String message, Long referenceId) {
        if (user == null) {
            log.warn("[NotificationService] Cannot send notification: recipient user is null");
            return;
        }
        try {
            Notification notification = Notification.builder()
                    .user(user)
                    .notificationType(type)
                    .title(title)
                    .message(message)
                    .referenceId(referenceId)
                    .readStatus(false)
                    .build();
            notificationRepository.save(notification);
            log.debug("[NotificationService] Sent {} notification to user id={}", type, user.getId());
        } catch (Exception e) {
            log.warn("[NotificationService] Failed to send notification to user id={}: {}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Sends a notification to all active users with a specific role.
     */
    @Transactional
    public void sendToRole(UserRole role, NotificationType type, String title, String message, Long referenceId) {
        try {
            List<User> users = userRepository.findByRole(role);
            for (User u : users) {
                send(u, type, title, message, referenceId);
            }
            log.debug("[NotificationService] Sent {} notification to {} users with role {}", type, users.size(), role);
        } catch (Exception e) {
            log.warn("[NotificationService] Failed to send notification to role {}: {}", role, e.getMessage());
        }
    }

    /**
     * Broadcasts a notification to all enabled platform users.
     */
    @Transactional
    public void broadcast(NotificationType type, String title, String message, Long referenceId) {
        try {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (u.isEnabled()) {
                    send(u, type, title, message, referenceId);
                }
            }
            log.debug("[NotificationService] Broadcast {} notification to {} users", type, users.size());
        } catch (Exception e) {
            log.warn("[NotificationService] Failed to broadcast notification: {}", e.getMessage());
        }
    }

    /**
     * Returns paginated notifications for the currently authenticated user with optional filter (ALL, UNREAD).
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String filter, int page, int size) {
        User user = resolveCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        
        if ("UNREAD".equalsIgnoreCase(filter)) {
            return notificationRepository
                    .findByUserIdAndReadStatusOrderByCreatedAtDesc(user.getId(), false, pageable)
                    .map(this::toResponse);
        }
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    /** Returns count of unread notifications for the current user. */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = resolveCurrentUser();
        return notificationRepository.countByUserIdAndReadStatus(user.getId(), false);
    }

    /** Marks all notifications as read for the current user. */
    @Transactional
    public int markAllRead() {
        User user = resolveCurrentUser();
        return notificationRepository.markAllReadByUserId(user.getId());
    }

    /** Marks a single notification as read. */
    @Transactional
    public void markRead(Long notificationId) {
        User user = resolveCurrentUser();
        Notification n = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));
        n.setReadStatus(true);
        notificationRepository.save(n);
    }

    /** Deletes a single notification owned by current user. */
    @Transactional
    public void deleteNotification(Long notificationId) {
        User user = resolveCurrentUser();
        Notification n = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));
        notificationRepository.delete(n);
    }

    /** Deletes all notifications for the current user. */
    @Transactional
    public int deleteAllNotifications() {
        User user = resolveCurrentUser();
        return notificationRepository.deleteAllByUserId(user.getId());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User resolveCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .readStatus(n.isReadStatus())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}


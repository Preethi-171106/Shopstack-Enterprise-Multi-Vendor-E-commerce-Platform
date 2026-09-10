package com.shopstack.controller;

import com.shopstack.dto.notification.NotificationResponse;
import com.shopstack.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * NotificationController — in-app notification endpoints.
 *
 * <p>Base URL: {@code /api/notifications}
 * <p>Requires any authenticated role (CUSTOMER, VENDOR, WAREHOUSE_STAFF, ADMIN).
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get paginated notifications for the current user")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false, defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(filter, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count for the current user")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount()));
    }

    @PatchMapping({"/read-all", "/mark-all-read"})
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        int updated = notificationService.markAllRead();
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete all notifications for current user")
    public ResponseEntity<Map<String, Integer>> deleteAllNotifications() {
        int deleted = notificationService.deleteAllNotifications();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}


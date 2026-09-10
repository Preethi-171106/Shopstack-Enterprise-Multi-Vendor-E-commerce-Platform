package com.shopstack.notification.controller;

import com.shopstack.notification.dto.BroadcastNotificationRequest;
import com.shopstack.notification.dto.CreateNotificationRequest;
import com.shopstack.notification.dto.NotificationResponse;
import com.shopstack.notification.dto.SystemNotificationRequest;
import com.shopstack.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAllNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.getAllNotifications(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @PostMapping("/system")
    public ResponseEntity<NotificationResponse> sendSystemNotification(@Valid @RequestBody SystemNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendSystemNotification(request, request.userId()));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<List<NotificationResponse>> broadcast(@Valid @RequestBody BroadcastNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.broadcast(request));
    }
}

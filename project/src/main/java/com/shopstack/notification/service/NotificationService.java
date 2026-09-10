package com.shopstack.notification.service;

import com.shopstack.common.entity.User;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.common.repository.UserRepository;
import com.shopstack.notification.dto.BroadcastNotificationRequest;
import com.shopstack.notification.dto.CreateNotificationRequest;
import com.shopstack.notification.dto.NotificationResponse;
import com.shopstack.notification.dto.SystemNotificationRequest;
import com.shopstack.notification.entity.Notification;
import com.shopstack.notification.enums.NotificationChannel;
import com.shopstack.notification.enums.NotificationStatus;
import com.shopstack.notification.enums.NotificationType;
import com.shopstack.notification.enums.ReferenceType;
import com.shopstack.notification.exception.NotificationNotFoundException;
import com.shopstack.notification.mapper.NotificationMapper;
import com.shopstack.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD, pageable)
                .map(NotificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationForUser(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You do not have access to this notification");
        }
        return NotificationMapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return NotificationMapper.toResponse(notification);
    }

    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You do not have access to this notification");
        }
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return NotificationMapper.toResponse(notification);
    }

    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, NotificationStatus.READ, Instant.now(), NotificationStatus.UNREAD);
    }

    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You do not have access to this notification");
        }
        notificationRepository.delete(notification);
    }

    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(NotificationMapper::toResponse);
    }

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        NotificationType type = parseEnum(NotificationType.class, request.notificationType(), "notificationType");
        NotificationChannel channel = request.channel() != null
                ? parseEnum(NotificationChannel.class, request.channel(), "channel")
                : NotificationChannel.IN_APP;
        ReferenceType referenceType = request.referenceType() != null
                ? parseEnum(ReferenceType.class, request.referenceType(), "referenceType")
                : null;

        Notification notification = Notification.builder()
                .user(user)
                .title(request.title())
                .message(request.message())
                .notificationType(type)
                .channel(channel)
                .status(NotificationStatus.UNREAD)
                .referenceId(request.referenceId())
                .referenceType(referenceType)
                .build();

        return NotificationMapper.toResponse(notificationRepository.save(notification));
    }

    public NotificationResponse sendSystemNotification(SystemNotificationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(request.title())
                .message(request.message())
                .notificationType(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .referenceType(ReferenceType.SYSTEM)
                .build();

        return NotificationMapper.toResponse(notificationRepository.save(notification));
    }

    public List<NotificationResponse> broadcast(BroadcastNotificationRequest request) {
        List<User> users = userRepository.findAll();
        NotificationType type = request.notificationType() != null
                ? parseEnum(NotificationType.class, request.notificationType(), "notificationType")
                : NotificationType.SYSTEM;

        return users.stream()
                .map(user -> {
                    Notification notification = Notification.builder()
                            .user(user)
                            .title(request.title())
                            .message(request.message())
                            .notificationType(type)
                            .channel(NotificationChannel.IN_APP)
                            .status(NotificationStatus.UNREAD)
                            .referenceType(ReferenceType.SYSTEM)
                            .build();
                    return NotificationMapper.toResponse(notificationRepository.save(notification));
                })
                .toList();
    }

    // ===== Auto-creation hooks for business events =====

    public void notifyOrderPlaced(Long userId, String orderReferenceId) {
        createForUser(userId, "Order Placed", "Your order has been placed successfully.",
                NotificationType.ORDER_PLACED, ReferenceType.ORDER, orderReferenceId);
    }

    public void notifyPaymentSuccess(Long userId, String paymentReferenceId) {
        createForUser(userId, "Payment Successful", "Your payment has been processed successfully.",
                NotificationType.PAYMENT_SUCCESS, ReferenceType.PAYMENT, paymentReferenceId);
    }

    public void notifyPaymentFailed(Long userId, String paymentReferenceId) {
        createForUser(userId, "Payment Failed", "Your payment could not be processed. Please try again.",
                NotificationType.PAYMENT_FAILED, ReferenceType.PAYMENT, paymentReferenceId);
    }

    public void notifyShipmentCreated(Long userId, String shipmentReferenceId) {
        createForUser(userId, "Order Shipped", "Your order has been shipped.",
                NotificationType.ORDER_SHIPPED, ReferenceType.SHIPMENT, shipmentReferenceId);
    }

    public void notifyShipmentDelivered(Long userId, String shipmentReferenceId) {
        createForUser(userId, "Order Delivered", "Your order has been delivered.",
                NotificationType.ORDER_DELIVERED, ReferenceType.SHIPMENT, shipmentReferenceId);
    }

    public void notifyCouponReceived(Long userId, String couponReferenceId) {
        createForUser(userId, "Coupon Received", "A coupon has been assigned to your account.",
                NotificationType.COUPON_RECEIVED, ReferenceType.COUPON, couponReferenceId);
    }

    public void notifyLowStock(Long userId, String productReferenceId) {
        createForUser(userId, "Low Stock Alert", "A product in your inventory is running low on stock.",
                NotificationType.LOW_STOCK, ReferenceType.PRODUCT, productReferenceId);
    }

    public void notifyReturnRequested(Long userId, String returnReferenceId) {
        createForUser(userId, "Return Requested", "Your return request has been submitted.",
                NotificationType.RETURN_REQUESTED, ReferenceType.RETURN, returnReferenceId);
    }

    public void notifyRefundCompleted(Long userId, String refundReferenceId) {
        createForUser(userId, "Refund Completed", "Your refund has been processed successfully.",
                NotificationType.REFUND_SUCCESS, ReferenceType.REFUND, refundReferenceId);
    }

    private void createForUser(Long userId, String title, String message,
                               NotificationType type, ReferenceType referenceType, String referenceId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + fieldName + ": " + value);
        }
    }
}

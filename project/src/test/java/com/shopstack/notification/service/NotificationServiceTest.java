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
import com.shopstack.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User customer;
    private User admin;
    private Notification notification;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@shopstack.com")
                .password("encoded")
                .firstName("Cust")
                .lastName("Omer")
                .phone("1234567890")
                .roles(Set.of("ROLE_CUSTOMER"))
                .build();

        admin = User.builder()
                .id(2L)
                .email("admin@shopstack.com")
                .password("encoded")
                .firstName("Ad")
                .lastName("Min")
                .phone("0987654321")
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        notification = Notification.builder()
                .id(10L)
                .user(customer)
                .title("Order Placed")
                .message("Your order has been placed.")
                .notificationType(NotificationType.ORDER_PLACED)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .referenceId("ORD-100")
                .referenceType(ReferenceType.ORDER)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getNotificationsForUser_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotificationsForUser(1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Order Placed", result.getContent().get(0).title());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getUnreadNotificationsForUser_returnsOnlyUnread() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.UNREAD, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUnreadNotificationsForUser(1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("UNREAD", result.getContent().get(0).status());
    }

    @Test
    void getNotificationForUser_returnsNotification() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.getNotificationForUser(1L, 10L);

        assertEquals(10L, result.id());
        assertEquals("Order Placed", result.title());
    }

    @Test
    void getNotificationForUser_otherUser_throwsUnauthorized() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(UnauthorizedActionException.class,
                () -> notificationService.getNotificationForUser(99L, 10L));
    }

    @Test
    void getNotificationForUser_notFound_throws() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.getNotificationForUser(1L, 999L));
    }

    @Test
    void markAsRead_updatesStatusAndReadAt() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse result = notificationService.markAsRead(1L, 10L);

        assertEquals("READ", result.status());
        assertNotNull(result.readAt());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsRead_alreadyRead_doesNotUpdateAgain() {
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.markAsRead(1L, 10L);

        assertEquals("READ", result.status());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_otherUser_throwsUnauthorized() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(UnauthorizedActionException.class,
                () -> notificationService.markAsRead(99L, 10L));
    }

    @Test
    void markAllAsRead_callsRepository() {
        when(notificationRepository.markAllAsRead(eq(1L), eq(NotificationStatus.READ), any(Instant.class), eq(NotificationStatus.UNREAD)))
                .thenReturn(3);

        int count = notificationService.markAllAsRead(1L);

        assertEquals(3, count);
        verify(notificationRepository).markAllAsRead(eq(1L), eq(NotificationStatus.READ), any(Instant.class), eq(NotificationStatus.UNREAD));
    }

    @Test
    void deleteNotification_owner_success() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 10L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_otherUser_throwsUnauthorized() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(UnauthorizedActionException.class,
                () -> notificationService.deleteNotification(99L, 10L));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void deleteAllNotifications_callsRepository() {
        when(notificationRepository.deleteAllByUserId(1L)).thenReturn(5);

        notificationService.deleteAllNotifications(1L);

        verify(notificationRepository).deleteAllByUserId(1L);
    }

    @Test
    void createNotification_validRequest_saves() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                1L, "Title", "Message", "ORDER_PLACED", "IN_APP", "ORD-1", "ORDER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(20L);
            return n;
        });

        NotificationResponse result = notificationService.createNotification(request);

        assertEquals(20L, result.id());
        assertEquals("ORDER_PLACED", result.notificationType());
        assertEquals("IN_APP", result.channel());
        assertEquals("UNREAD", result.status());
    }

    @Test
    void createNotification_invalidType_throwsBadRequest() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                1L, "Title", "Message", "INVALID_TYPE", "IN_APP", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () -> notificationService.createNotification(request));
    }

    @Test
    void createNotification_userNotFound_throws() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                999L, "Title", "Message", "SYSTEM", "IN_APP", null, null);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.createNotification(request));
    }

    @Test
    void sendSystemNotification_saves() {
        SystemNotificationRequest request = new SystemNotificationRequest(1L, "System Msg", "Hello");
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(30L);
            return n;
        });

        NotificationResponse result = notificationService.sendSystemNotification(request, 1L);

        assertEquals(30L, result.id());
        assertEquals("SYSTEM", result.notificationType());
    }

    @Test
    void broadcast_createsNotificationForEachUser() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest("Promo", "Sale!", "PROMOTION");
        when(userRepository.findAll()).thenReturn(List.of(customer, admin));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(System.nanoTime());
            return n;
        });

        List<NotificationResponse> result = notificationService.broadcast(request);

        assertEquals(2, result.size());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void notifyOrderPlaced_savesNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        notificationService.notifyOrderPlaced(1L, "ORD-100");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyPaymentSuccess_savesNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        notificationService.notifyPaymentSuccess(1L, "PAY-100");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyLowStock_savesNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        notificationService.notifyLowStock(1L, "PROD-100");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyOrderPlaced_userNotFound_doesNotThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.notifyOrderPlaced(999L, "ORD-100"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}

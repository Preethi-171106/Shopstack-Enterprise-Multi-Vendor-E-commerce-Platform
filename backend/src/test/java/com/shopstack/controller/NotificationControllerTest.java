package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.entity.Notification;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.repository.NotificationRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.security.JwtService;
import com.shopstack.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;

    private Notification notif1User1;
    private Notification notif2User1;
    private Notification notif1User2;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Customer")
                .email("alice-" + UUID.randomUUID() + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());

        user2 = userRepository.save(User.builder()
                .firstName("Bob")
                .lastName("Customer")
                .email("bob-" + UUID.randomUUID() + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build());

        user1Token = "Bearer " + jwtService.generateToken(user1);
        user2Token = "Bearer " + jwtService.generateToken(user2);

        notif1User1 = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Order Placed")
                .message("Order #1001 placed successfully")
                .notificationType(NotificationType.ORDER_PLACED)
                .readStatus(false)
                .referenceId(1001L)
                .build());

        notif2User1 = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Order Shipped")
                .message("Order #1001 has been shipped")
                .notificationType(NotificationType.ORDER_SHIPPED)
                .readStatus(true)
                .referenceId(1001L)
                .build());

        notif1User2 = notificationRepository.save(Notification.builder()
                .user(user2)
                .title("Bob's Notification")
                .message("Message for Bob")
                .notificationType(NotificationType.GENERAL)
                .readStatus(false)
                .build());
    }

    @Nested
    @DisplayName("GET /api/notifications & /unread-count")
    class GetNotificationsTests {

        @Test
        @DisplayName("Authenticated user gets their own paginated notifications (Isolation enforced)")
        void getMyNotifications_ReturnsUserNotificationsOnly() throws Exception {
            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", user1Token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.content[0].title").exists());
        }

        @Test
        @DisplayName("Filter UNREAD returns only unread notifications")
        void getUnreadNotifications_ReturnsOnlyUnread() throws Exception {
            mockMvc.perform(get("/api/notifications?filter=UNREAD")
                            .header("Authorization", user1Token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(notif1User1.getId().intValue())))
                    .andExpect(jsonPath("$.content[0].readStatus", is(false)));
        }

        @Test
        @DisplayName("GET /api/notifications/unread-count returns accurate count")
        void getUnreadCount_ReturnsAccurateCount() throws Exception {
            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount", is(1)));

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount", is(1)));
        }

        @Test
        @DisplayName("Unauthenticated request returns 401 Unauthorized")
        void unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/notifications/unread-count"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{id}/read & /read-all")
    class MarkReadTests {

        @Test
        @DisplayName("Mark single notification as read succeeds")
        void markRead_Succeeds() throws Exception {
            mockMvc.perform(patch("/api/notifications/" + notif1User1.getId() + "/read")
                            .header("Authorization", user1Token))
                    .andExpect(status().isNoContent());

            Notification updated = notificationRepository.findById(notif1User1.getId()).orElseThrow();
            assert updated.isReadStatus();
        }

        @Test
        @DisplayName("Marking another user's notification as read returns 404 Not Found")
        void markRead_AnotherUser_Returns404() throws Exception {
            mockMvc.perform(patch("/api/notifications/" + notif1User2.getId() + "/read")
                            .header("Authorization", user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Mark all as read updates all unread notifications for current user")
        void markAllRead_Succeeds() throws Exception {
            mockMvc.perform(patch("/api/notifications/read-all")
                            .header("Authorization", user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.markedRead", is(1)));

            long unread = notificationRepository.countByUserIdAndReadStatus(user1.getId(), false);
            assert unread == 0;

            // Bob's unread is unaffected
            long bobUnread = notificationRepository.countByUserIdAndReadStatus(user2.getId(), false);
            assert bobUnread == 1;
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications/{id} & /api/notifications")
    class DeleteNotificationsTests {

        @Test
        @DisplayName("Delete single notification succeeds")
        void deleteSingle_Succeeds() throws Exception {
            mockMvc.perform(delete("/api/notifications/" + notif1User1.getId())
                            .header("Authorization", user1Token))
                    .andExpect(status().isNoContent());

            assert notificationRepository.findById(notif1User1.getId()).isEmpty();
        }

        @Test
        @DisplayName("Delete another user's notification returns 404 Not Found")
        void deleteAnotherUser_Returns404() throws Exception {
            mockMvc.perform(delete("/api/notifications/" + notif1User2.getId())
                            .header("Authorization", user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete all notifications for current user succeeds")
        void deleteAll_Succeeds() throws Exception {
            mockMvc.perform(delete("/api/notifications")
                            .header("Authorization", user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted", is(2)));

            // User 1 notifications gone
            assert notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements() == 0;
            // Bob's notification intact
            assert notificationRepository.findByUserIdOrderByCreatedAtDesc(user2.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements() == 1;
        }
    }

    @Nested
    @DisplayName("Service-level Notification Dispatch Tests")
    class NotificationDispatchTests {

        @Test
        @DisplayName("sendToRole dispatches to all users with matching role")
        void sendToRole_DispatchesToRole() {
            notificationService.sendToRole(
                    UserRole.CUSTOMER,
                    NotificationType.SYSTEM,
                    "System Maintenance",
                    "Scheduled maintenance at midnight.",
                    null
            );

            long user1Count = notificationRepository.countByUserIdAndReadStatus(user1.getId(), false);
            long user2Count = notificationRepository.countByUserIdAndReadStatus(user2.getId(), false);

            assert user1Count >= 2;
            assert user2Count >= 2;
        }

        @Test
        @DisplayName("broadcast sends notification to all enabled platform users")
        void broadcast_SendsToAll() {
            notificationService.broadcast(
                    NotificationType.PROMOTION,
                    "Grand Sale",
                    "Enjoy up to 50% discount this weekend!",
                    null
            );

            assert notificationRepository.findAll().stream()
                    .anyMatch(n -> n.getTitle().equals("Grand Sale"));
        }
    }
}

package com.shopstack.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.notification.dto.NotificationResponse;
import com.shopstack.notification.service.NotificationService;
import com.shopstack.security.JwtAuthEntryPoint;
import com.shopstack.security.JwtAuthenticationFilter;
import com.shopstack.security.JwtTokenProvider;
import com.shopstack.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private SecurityUtils securityUtils;

    private NotificationResponse response;

    @BeforeEach
    void setUp() {
        response = new NotificationResponse(
                10L, 1L, "Order Placed", "Your order has been placed.",
                "ORDER_PLACED", "IN_APP", "UNREAD", "ORD-100", "ORDER",
                Instant.now(), null);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyNotifications_returns200() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(notificationService.getNotificationsForUser(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("Order Placed"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getUnreadNotifications_returns200() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(notificationService.getUnreadNotificationsForUser(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("UNREAD"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getNotification_returns200() throws Exception {
        when(notificationService.getNotificationForUser(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/notifications/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getNotification_notFound_returns404() throws Exception {
        when(notificationService.getNotificationForUser(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Notification", 999L));

        mockMvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getNotification_otherOwner_returns403() throws Exception {
        when(notificationService.getNotificationForUser(1L, 10L))
                .thenThrow(new UnauthorizedActionException("You do not have access to this notification"));

        mockMvc.perform(get("/api/notifications/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void markAsRead_returns200() throws Exception {
        NotificationResponse readResponse = new NotificationResponse(
                10L, 1L, "Order Placed", "Your order has been placed.",
                "ORDER_PLACED", "IN_APP", "READ", "ORD-100", "ORDER",
                Instant.now(), Instant.now());
        when(notificationService.markAsRead(1L, 10L)).thenReturn(readResponse);

        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void markAsRead_otherOwner_returns403() throws Exception {
        when(notificationService.markAsRead(1L, 10L))
                .thenThrow(new UnauthorizedActionException("access denied"));

        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void markAllAsRead_returns204() throws Exception {
        when(notificationService.markAllAsRead(1L)).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteNotification_returns204() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L, 10L);

        mockMvc.perform(delete("/api/notifications/10"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteNotification_otherOwner_returns403() throws Exception {
        doThrow(new UnauthorizedActionException("access denied"))
                .when(notificationService).deleteNotification(1L, 10L);

        mockMvc.perform(delete("/api/notifications/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteAllNotifications_returns204() throws Exception {
        doNothing().when(notificationService).deleteAllNotifications(1L);

        mockMvc.perform(delete("/api/notifications"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteAllNotifications(1L);
    }

    @Test
    void getMyNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}

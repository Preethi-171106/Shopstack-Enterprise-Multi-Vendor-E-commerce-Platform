package com.shopstack.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.notification.dto.BroadcastNotificationRequest;
import com.shopstack.notification.dto.NotificationResponse;
import com.shopstack.notification.dto.SystemNotificationRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminNotificationController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class AdminNotificationControllerTest {

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
                10L, 1L, "System", "System message",
                "SYSTEM", "IN_APP", "UNREAD", null, "SYSTEM",
                Instant.now(), null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllNotifications_returns200() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(notificationService.getAllNotifications(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getNotification_returns200() throws Exception {
        when(notificationService.getNotificationById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/notifications/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getNotification_notFound_returns404() throws Exception {
        when(notificationService.getNotificationById(999L))
                .thenThrow(new ResourceNotFoundException("Notification", 999L));

        mockMvc.perform(get("/api/admin/notifications/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendSystemNotification_returns201() throws Exception {
        SystemNotificationRequest request = new SystemNotificationRequest(1L, "System Msg", "Hello users");
        when(notificationService.sendSystemNotification(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/admin/notifications/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("System"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendSystemNotification_blankTitle_returns400() throws Exception {
        SystemNotificationRequest request = new SystemNotificationRequest(1L, "", "Hello users");

        mockMvc.perform(post("/api/admin/notifications/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendSystemNotification_blankMessage_returns400() throws Exception {
        SystemNotificationRequest request = new SystemNotificationRequest(1L, "Title", "");

        mockMvc.perform(post("/api/admin/notifications/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void broadcast_returns201() throws Exception {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest("Promo", "Sale!", "PROMOTION");
        NotificationResponse r1 = new NotificationResponse(1L, 1L, "Promo", "Sale!", "PROMOTION",
                "IN_APP", "UNREAD", null, "SYSTEM", Instant.now(), null);
        NotificationResponse r2 = new NotificationResponse(2L, 2L, "Promo", "Sale!", "PROMOTION",
                "IN_APP", "UNREAD", null, "SYSTEM", Instant.now(), null);
        when(notificationService.broadcast(any())).thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].title").value("Promo"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void broadcast_blankTitle_returns400() throws Exception {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest("", "Sale!", "PROMOTION");

        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isUnauthorized());
    }
}

package com.shopstack.controller;

import com.shopstack.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public Endpoint — /api/health returns 200 OK without token")
    void publicHealthEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Unauthenticated — /api/test/customer returns 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/test/customer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role — /api/test/customer returns 200 OK")
    void customerRole_AccessCustomerEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/test/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role — /api/test/admin returns 403 Forbidden")
    void customerRole_AccessAdminEndpoint_Returns403() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ADMIN role — /api/test/admin returns 200 OK")
    void adminRole_AccessAdminEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ADMIN role — /api/test/vendor returns 403 Forbidden")
    void adminRole_AccessVendorEndpoint_Returns403() throws Exception {
        mockMvc.perform(get("/api/test/vendor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "vendor@example.com", roles = {"VENDOR"})
    @DisplayName("VENDOR role — /api/test/vendor returns 200 OK")
    void vendorRole_AccessVendorEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/test/vendor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VENDOR"));
    }

    @Test
    @WithMockUser(username = "warehouse@example.com", roles = {"WAREHOUSE_STAFF"})
    @DisplayName("WAREHOUSE_STAFF role — /api/test/warehouse returns 200 OK")
    void warehouseRole_AccessWarehouseEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/test/warehouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("WAREHOUSE_STAFF"));
    }
}

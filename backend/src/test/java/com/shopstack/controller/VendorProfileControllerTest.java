package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.VendorProfileCreateRequest;
import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.dto.VendorProfileUpdateRequest;
import com.shopstack.entity.VendorStatus;
import com.shopstack.exception.VendorProfileAlreadyExistsException;
import com.shopstack.exception.VendorProfileNotFoundException;
import com.shopstack.service.VendorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VendorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendorService vendorService;

    @Test
    @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
    @DisplayName("VENDOR role — POST /api/vendors/profile creates profile (201 Created)")
    void vendorCreateProfile_Success() throws Exception {
        VendorProfileCreateRequest createReq = new VendorProfileCreateRequest(
                "TechWorld Store",
                "Electronics Store Description",
                "business@techworld.com",
                "9876543210",
                "123 Market Road",
                "Chennai",
                "Tamil Nadu",
                "India",
                "600001"
        );

        VendorProfileResponse response = new VendorProfileResponse(
                1L,
                2L,
                "TechWorld Store",
                "Electronics Store Description",
                "business@techworld.com",
                "9876543210",
                "123 Market Road",
                "Chennai",
                "Tamil Nadu",
                "India",
                "600001",
                VendorStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(vendorService.createVendorProfile(eq("vendor@shopstack.com"), any(VendorProfileCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/vendors/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeName").value("TechWorld Store"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
    @DisplayName("VENDOR role — GET /api/vendors/me retrieves own profile (200 OK)")
    void vendorGetProfile_Success() throws Exception {
        VendorProfileResponse response = new VendorProfileResponse(
                1L,
                2L,
                "TechWorld Store",
                "Electronics Store Description",
                "business@techworld.com",
                "9876543210",
                "123 Market Road",
                "Chennai",
                "Tamil Nadu",
                "India",
                "600001",
                VendorStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(vendorService.getVendorProfile("vendor@shopstack.com")).willReturn(response);

        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("TechWorld Store"));
    }

    @Test
    @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
    @DisplayName("VENDOR role — PUT /api/vendors/me updates profile (200 OK)")
    void vendorUpdateProfile_Success() throws Exception {
        VendorProfileUpdateRequest updateReq = new VendorProfileUpdateRequest(
                "TechWorld Electronics",
                "Updated Description",
                "newbusiness@techworld.com",
                "9876543210",
                "Updated Address",
                "Chennai",
                "Tamil Nadu",
                "India",
                "600002"
        );

        VendorProfileResponse response = new VendorProfileResponse(
                1L,
                2L,
                "TechWorld Electronics",
                "Updated Description",
                "newbusiness@techworld.com",
                "9876543210",
                "Updated Address",
                "Chennai",
                "Tamil Nadu",
                "India",
                "600002",
                VendorStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(vendorService.updateVendorProfile(eq("vendor@shopstack.com"), any(VendorProfileUpdateRequest.class)))
                .willReturn(response);

        mockMvc.perform(put("/api/vendors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("TechWorld Electronics"));
    }

    @Test
    @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
    @DisplayName("Duplicate creation attempt returns 409 Conflict")
    void duplicateProfileCreation_Returns409() throws Exception {
        VendorProfileCreateRequest createReq = new VendorProfileCreateRequest(
                "TechWorld Store",
                null, null, null, null, null, null, null, null
        );

        given(vendorService.createVendorProfile(eq("vendor@shopstack.com"), any(VendorProfileCreateRequest.class)))
                .willThrow(new VendorProfileAlreadyExistsException("Vendor profile already exists"));

        mockMvc.perform(post("/api/vendors/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role — /api/vendors/me returns 403 Forbidden")
    void customerAccessingVendorApi_Returns403() throws Exception {
        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Unauthenticated request — /api/vendors/me returns 401 Unauthorized")
    void unauthenticatedAccessingVendorApi_Returns401() throws Exception {
        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

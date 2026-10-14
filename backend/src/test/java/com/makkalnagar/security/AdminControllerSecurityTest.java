package com.makkalnagar.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Test 13: Unauthorized administrative requests return 401 Unauthorized")
    void testUnauthorizedAdminRequest() throws Exception {
        mockMvc.perform(get("/api/admin/complaints"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/departments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test: Public tracking endpoints allow anonymous access")
    void testPublicEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test: Authorized administrative requests succeed with ROLE_ADMIN")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAuthorizedAdminRequest() throws Exception {
        mockMvc.perform(get("/api/admin/complaints"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/departments"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk());
    }
}

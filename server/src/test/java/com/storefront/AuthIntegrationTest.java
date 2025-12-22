package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_auth;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public class AuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private com.storefront.service.AuthService authService;

        @Test
        void testAuthFlow() throws Exception {
                // 0. Login as SuperAdmin to get token for registration
                var superAdmin = authService.login("superadmin", "password")
                                .orElseThrow(() -> new RuntimeException("SuperAdmin login failed"));
                String superToken = authService.generateToken(superAdmin);

                String username = "testuser_" + System.currentTimeMillis();
                String password = "password123";

                // 1. Register (as Admin)
                Map<String, String> registerRequest = Map.of(
                                "username", username,
                                "password", password,
                                "role", "EMPLOYEE");

                mockMvc.perform(post("/api/v1/auth/register")
                                .header("Authorization", "Bearer " + superToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value(username));

                // 2. Login the newly created user
                var newUser = authService.login(username, password)
                                .orElseThrow(() -> new RuntimeException("New user login failed"));
                String token = authService.generateToken(newUser);

                // 3. Access Protected Resource
                mockMvc.perform(get("/api/v1/auth/users") // Use an existing endpoint to test auth
                                .header("Authorization", "Bearer " + superToken)) // List users requires Admin
                                .andExpect(status().isOk());
        }

        @Test
        void testDeleteUserFlow() throws Exception {
                // Login SuperAdmin
                var superAdmin = authService.login("superadmin", "password")
                                .orElseThrow(() -> new RuntimeException("SuperAdmin login failed"));
                String superToken = authService.generateToken(superAdmin);

                // Create Admin for this test (using superadmin token)
                mockMvc.perform(post("/api/v1/auth/register")
                                .header("Authorization", "Bearer " + superToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("username", "admin_del", "password", "pass", "role", "ADMIN"))))
                                .andExpect(status().isOk());

                // Login Admin
                var adminUser = authService.login("admin_del", "pass")
                                .orElseThrow(() -> new RuntimeException("Admin login failed"));
                String adminToken = authService.generateToken(adminUser);

                // Register Victim (using admin token)
                mockMvc.perform(post("/api/v1/auth/register")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("username", "victim", "password", "pass", "role", "EMPLOYEE"))))
                                .andExpect(status().isOk());

                // Get Victim ID (users list)
                String usersResp = mockMvc.perform(get("/api/v1/auth/users")
                                .header("Authorization", "Bearer " + adminToken))
                                .andReturn().getResponse().getContentAsString();

                com.fasterxml.jackson.databind.JsonNode usersNode = objectMapper.readTree(usersResp);
                int victimId = 0;
                for (com.fasterxml.jackson.databind.JsonNode node : usersNode) {
                        if (node.get("username").asText().equals("victim")) {
                                victimId = node.get("id").asInt();
                                break;
                        }
                }

                // Delete Victim
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/auth/users/" + victimId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
        }
}

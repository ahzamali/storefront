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

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAuthFlow() throws Exception {
        String username = "testuser_" + System.currentTimeMillis();
        String password = "password123";

        // 1. Register
        Map<String, String> registerRequest = Map.of(
                "username", username,
                "password", password,
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        // 2. Login
        Map<String, String> loginRequest = Map.of(
                "username", username,
                "password", password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // 3. Access Protected Resource (Assuming /api/v1/unknown-endpoint yields 404
        // but 403 if unauthorized)
        // Wait, I haven't defined any other endpoints yet.
        // Let's rely on the fact that any request to /api/v1/auth is public, but others
        // are protected.
        // Let's try to access a fictitious endpoint which should be 404 if authorized,
        // but 403 if not.

        // Without Token -> 403 Forbidden (or 401 Unauthorized depending on config, but
        // default is usually 403/401)
        mockMvc.perform(get("/api/v1/protected-resource"))
                .andExpect(status().isForbidden());

        // With Token -> 404 Not Found (Authorized but resource missing)
        mockMvc.perform(get("/api/v1/protected-resource")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}

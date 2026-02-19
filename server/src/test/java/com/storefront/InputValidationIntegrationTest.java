package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.model.Role;
import com.storefront.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_input_val;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public class InputValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthService authService;

    private String adminToken;

    @BeforeEach
    void setup() {
        if (authService.login("admin_input", "pass").isEmpty()) {
            var admin = authService.register("admin_input", "pass", Role.ADMIN);
            adminToken = authService.generateToken(admin);
        } else {
            adminToken = authService.generateToken(authService.login("admin_input", "pass").get());
        }
    }

    // Authentication Validation Tests
    @Test
    void register_NullUsername_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "password", "Password123!",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username is required"));
    }

    @Test
    void register_EmptyUsername_ReturnsBadRequest() throws Exception {
        Map<String, String> request = Map.of(
                "username", "",
                "password", "Password123!",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username must be 3-50 characters"));
    }

    @Test
    void register_ShortUsername_ReturnsBadRequest() throws Exception {
        Map<String, String> request = Map.of(
                "username", "ab",
                "password", "Password123!",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username must be 3-50 characters"));
    }

    @Test
    void register_LongUsername_ReturnsBadRequest() throws Exception {
        Map<String, String> request = Map.of(
                "username", "a".repeat(51),
                "password", "Password123!",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username must be 3-50 characters"));
    }

    @Test
    void register_NullPassword_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "testuser",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password is required"));
    }

    @Test
    void register_EmptyPassword_ReturnsBadRequest() throws Exception {
        Map<String, String> request = Map.of(
                "username", "testuser",
                "password", "",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password is required"));
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        Map<String, String> request = Map.of(
                "username", "testuser",
                "password", "Pass1!",
                "role", "EMPLOYEE");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password must be at least 8 characters"));
    }

    @Test
    void register_DuplicateUsername_ReturnsConflict() throws Exception {
        authService.register("duplicate_user", "Password123!", Role.EMPLOYEE);

        Map<String, String> request = Map.of(
                "username", "duplicate_user",
                "password", "Password123!",
                "role", "EMPLOYEE");

        var result = mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        System.out.println("Response: " + result.andReturn().getResponse().getContentAsString());
        
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_EmptyRole_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "testuser",
                "password", "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.role").value("Role is required"));
    }

    // Order Validation Tests
    @Test
    void createOrder_NullStoreId_ReturnsBadRequest() throws Exception {
        String orderJson = """
                {
                    "items": [
                        {"sku": "SKU1", "quantity": 1}
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.storeId").value("Store ID is required"));
    }

    @Test
    void createOrder_EmptyItems_ReturnsBadRequest() throws Exception {
        String orderJson = """
                {
                    "storeId": 1,
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.items").value("Order must contain at least one item"));
    }

    @Test
    void createOrder_NullSku_ReturnsBadRequest() throws Exception {
        String orderJson = """
                {
                    "storeId": 1,
                    "items": [
                        {"quantity": 1}
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['items[0].sku']").value("SKU is required"));
    }

    @Test
    void createOrder_ZeroQuantity_ReturnsBadRequest() throws Exception {
        String orderJson = """
                {
                    "storeId": 1,
                    "items": [
                        {"sku": "SKU1", "quantity": 0}
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['items[0].quantity']").value("Quantity must be at least 1"));
    }

    @Test
    void createOrder_NegativeQuantity_ReturnsBadRequest() throws Exception {
        String orderJson = """
                {
                    "storeId": 1,
                    "items": [
                        {"sku": "SKU1", "quantity": -5}
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['items[0].quantity']").value("Quantity must be at least 1"));
    }
}

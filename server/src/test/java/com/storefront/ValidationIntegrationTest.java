package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.AllocationRequestDTO;
import com.storefront.model.Role;
import com.storefront.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_val;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthService authService;

    private String adminToken;

    @BeforeEach
    void setup() {
        if (authService.login("admin_val", "pass").isEmpty()) {
            var admin = authService.register("admin_val", "pass", Role.ADMIN);
            adminToken = authService.generateToken(admin);
        } else {
            adminToken = authService.generateToken(authService.login("admin_val", "pass").get());
        }
    }

    @Test
    void testCreateProduct_InvalidPrice() throws Exception {
        Map<String, Object> product = Map.of(
                "sku", "INV-PRICE",
                "name", "Invalid Price",
                "category", "TEST",
                "price", -10.0);

        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_MissingName() throws Exception {
        Map<String, Object> product = Map.of(
                "sku", "INV-NAME",
                "category", "TEST",
                "price", 10.0);

        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAllocate_NegativeQuantity() throws Exception {
        String allocationJson = """
                    {
                        "items": [
                            { "sku": "VALID-SKU", "quantity": -5 }
                        ]
                    }
                """;

        mockMvc.perform(post("/api/v1/stores/1/allocate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(allocationJson))
                .andExpect(status().isBadRequest());
    }
}

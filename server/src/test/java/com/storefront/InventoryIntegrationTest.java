package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.BundleDTO;
import com.storefront.dto.StockIngestDTO;
import com.storefront.model.Product;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.StoreRepository;
import com.storefront.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private StoreRepository storeRepository;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setup() {
        // Ensure Master Store exists
        if (storeRepository.findFirstByType(Store.StoreType.MASTER).isEmpty()) {
            storeRepository.save(new Store("Master Warehouse", Store.StoreType.MASTER, null));
        }

        // Create Admin
        try {
            var admin = authService.register("admin_inv", "pass", Role.ADMIN);
            adminToken = authService.generateToken(admin);
        } catch (Exception e) {
            // might already exist
            adminToken = authService.login("admin_inv", "pass").map(u -> authService.generateToken(u)).orElse("");
        }

        // Create Employee
        try {
            var emp = authService.register("emp_inv", "pass", Role.EMPLOYEE);
            employeeToken = authService.generateToken(emp);
        } catch (Exception e) {
            employeeToken = authService.login("emp_inv", "pass").map(u -> authService.generateToken(u)).orElse("");
        }
    }

    @Test
    void testInventoryFlow() throws Exception {
        // 1. Create Product (Admin) -> 200
        Product product = new Product("SKU-INV-1", "BOOK", "Inventory Book", new BigDecimal("20.00"), null);
        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        // 2. Create Product (Employee) -> 403
        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isForbidden());

        // 3. Create Bundle (Admin) -> 200
        BundleDTO bundleDTO = new BundleDTO();
        bundleDTO.setSku("BUN-INV-1");
        bundleDTO.setName("Test Bundle");
        bundleDTO.setPrice(new BigDecimal(50));

        BundleDTO.BundleItemDTO item = new BundleDTO.BundleItemDTO();
        item.setProductSku("SKU-INV-1");
        item.setQuantity(2);
        bundleDTO.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bundleDTO)))
                .andExpect(status().isOk());

        // 4. Add Stock (Admin) -> 200
        StockIngestDTO stockDTO = new StockIngestDTO();
        stockDTO.setSku("SKU-INV-1");
        stockDTO.setQuantity(100);

        mockMvc.perform(post("/api/v1/inventory/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(100));
    }
}

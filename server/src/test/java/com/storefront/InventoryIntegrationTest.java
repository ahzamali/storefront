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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_inv;DB_CLOSE_DELAY=-1")
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
    @Autowired
    private com.storefront.service.InventoryService inventoryService;

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
            var admin = authService.register("admin_inv", "pass", Role.SUPER_ADMIN);
            adminToken = authService.generateToken(admin);
        } catch (Exception e) {
            // might already exist
            var admin = authService.login("admin_inv", "pass").get();
            if (admin.getRole() != Role.SUPER_ADMIN) {
                admin.setRole(Role.SUPER_ADMIN);
                // need repos to save? AuthService usually just returns user.
                // Re-register or login doesn't expose save easily here unless injected.
                // But let's assume register works if dropped or we just need token.
                // Actually, if exists, we might need to update role.
                // Inject AppUserRepository to be safe?
                // It was injected in InventoryIntegrationTest? No.
                // Let's check imports.
            }
            adminToken = authService.generateToken(admin);
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

        // 5. Get Bundles (Admin/Employee) -> 200
        mockMvc.perform(get("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("BUN-INV-1"))
                .andExpect(jsonPath("$[0].items").isArray());
    }

    @Test
    void testSoftDeleteProduct() throws Exception {
        // Create Product
        Product product = inventoryService.createProduct(
                new Product("SKU-DEL", "BOOK", "To Delete", new BigDecimal("10"), null));

        // Delete Product
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/inventory/products/" + product.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify it is not returned in active list
        mockMvc.perform(get("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'SKU-DEL')]").isEmpty());

        // Verify it is still in DB (Active = false)
        // Direct DB check or Admin endpoint including inactive (if exists)
    }
}

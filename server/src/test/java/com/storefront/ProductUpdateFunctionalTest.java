package com.storefront;

import com.storefront.model.Product;
import com.storefront.model.Store;
import com.storefront.repository.ProductRepository;
import com.storefront.repository.StockLevelRepository;
import com.storefront.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductUpdateFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    private String adminToken;
    private Store masterStore;

    @BeforeEach
    void setup() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        adminToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];
        masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER).orElseThrow();
    }

    @Test
    void testUpdateProductDetails() throws Exception {
        Product product = createProduct("UPD-001", "Original Name", 100.0, "BOOK");

        String updateJson = """
            {
                "name": "Updated Name",
                "basePrice": 120.0,
                "type": "BOOK",
                "attributes": {
                    "author": "New Author",
                    "isbn": "1234567890"
                }
            }
            """;

        mockMvc.perform(put("/api/v1/inventory/products/" + product.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assert updated.getName().equals("Updated Name");
        assert updated.getBasePrice().doubleValue() == 120.0;
        assert updated.getAttributes().get("author").equals("New Author");
    }

    @Test
    void testUpdateStockCount() throws Exception {
        Product product = createProduct("UPD-002", "Stock Test", 50.0, "STATIONERY");
        addStock(product.getSku(), 100);

        String updateJson = """
            {
                "sku": "%s",
                "quantity": 150
            }
            """.formatted(product.getSku());

        mockMvc.perform(put("/api/v1/inventory/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        var stock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), product.getId()).orElseThrow();
        assert stock.getQuantity() == 150;
    }

    @Test
    void testUpdateStockForVirtualStore() throws Exception {
        Product product = createProduct("UPD-003", "Virtual Stock", 80.0, "BOOK");
        addStock(product.getSku(), 200);

        Store virtualStore = createStore("Virtual Store");
        allocateStock(virtualStore.getId(), product.getSku(), 50);

        String updateJson = """
            {
                "sku": "%s",
                "quantity": 30,
                "storeId": %d
            }
            """.formatted(product.getSku(), virtualStore.getId());

        mockMvc.perform(put("/api/v1/inventory/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        var virtualStock = stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), product.getId()).orElseThrow();
        assert virtualStock.getQuantity() == 30;

        var masterStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), product.getId()).orElseThrow();
        assert masterStock.getQuantity() == 150;
    }

    @Test
    void testUpdateProductWithInvalidPrice() throws Exception {
        Product product = createProduct("UPD-004", "Invalid Test", 100.0, "BOOK");

        String updateJson = """
            {
                "name": "Test",
                "basePrice": -50.0,
                "type": "BOOK"
            }
            """;

        mockMvc.perform(put("/api/v1/inventory/products/" + product.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateNonExistentProduct() throws Exception {
        String updateJson = """
            {
                "name": "Test",
                "basePrice": 100.0,
                "type": "BOOK"
            }
            """;

        mockMvc.perform(put("/api/v1/inventory/products/99999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProductAttributes() throws Exception {
        Product product = createProduct("UPD-005", "Attr Test", 70.0, "BOOK");

        String updateJson = """
            {
                "name": "Attr Test",
                "basePrice": 70.0,
                "type": "BOOK",
                "attributes": {
                    "author": "Test Author",
                    "isbn": "9876543210",
                    "publisher": "Test Publisher",
                    "publicationDate": "2024-01-01"
                }
            }
            """;

        mockMvc.perform(put("/api/v1/inventory/products/" + product.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assert updated.getAttributes().get("author").equals("Test Author");
        assert updated.getAttributes().get("publisher").equals("Test Publisher");
    }

    private Product createProduct(String sku, String name, double price, String type) throws Exception {
        String json = """
            {
                "sku": "%s",
                "name": "%s",
                "basePrice": %.2f,
                "type": "%s"
            }
            """.formatted(sku, name, price, type);

        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        return productRepository.findBySku(sku).orElseThrow();
    }

    private void addStock(String sku, int quantity) throws Exception {
        String json = """
            {
                "sku": "%s",
                "quantity": %d
            }
            """.formatted(sku, quantity);

        mockMvc.perform(post("/api/v1/inventory/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    private Store createStore(String name) throws Exception {
        String json = """
            {
                "name": "%s"
            }
            """.formatted(name);

        String response = mockMvc.perform(post("/api/v1/stores")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long storeId = Long.parseLong(response.split("\"id\":")[1].split(",")[0]);
        return storeRepository.findById(storeId).orElseThrow();
    }

    private void allocateStock(Long storeId, String sku, int quantity) throws Exception {
        String json = """
            {
                "items": [{"sku": "%s", "quantity": %d}]
            }
            """.formatted(sku, quantity);

        mockMvc.perform(post("/api/v1/stores/" + storeId + "/allocate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    private void createOrder(Long storeId, String customerName, String phone, String sku, int quantity) throws Exception {
        String orderJson = """
            {
                "storeId": %d,
                "customerName": "%s",
                "customerPhone": "%s",
                "items": [{"sku": "%s", "quantity": %d}]
            }
            """.formatted(storeId, customerName, phone, sku, quantity);

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk());
    }
}

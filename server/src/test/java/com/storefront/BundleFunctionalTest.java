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
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BundleFunctionalTest {

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
    void testCreateBundleSuccess() throws Exception {
        Product p1 = createProduct("BND-P1", "Math Book", 100.0);
        Product p2 = createProduct("BND-P2", "Blue Pen", 10.0);
        addStock(p1.getSku(), 50);
        addStock(p2.getSku(), 100);

        String bundleJson = """
            {
                "sku": "BUNDLE-001",
                "name": "Student Kit",
                "description": "Math book and pen",
                "price": 95.0,
                "items": [
                    {"productSku": "BND-P1", "quantity": 1},
                    {"productSku": "BND-P2", "quantity": 2}
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bundleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("BUNDLE-001"))
                .andExpect(jsonPath("$.name").value("Student Kit"));
    }

    @Test
    void testBundleOrderDeductsIndividualItems() throws Exception {
        Product p1 = createProduct("BND-P3", "Science Book", 150.0);
        Product p2 = createProduct("BND-P4", "Red Pen", 15.0);
        addStock(p1.getSku(), 20);
        addStock(p2.getSku(), 50);

        String bundleJson = """
            {
                "sku": "BUNDLE-002",
                "name": "Science Kit",
                "price": 160.0,
                "items": [
                    {"productSku": "BND-P3", "quantity": 1},
                    {"productSku": "BND-P4", "quantity": 3}
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bundleJson))
                .andExpect(status().isOk());

        String orderJson = """
            {
                "storeId": %d,
                "customerName": "John Doe",
                "customerPhone": "1234567890",
                "items": [{"sku": "BUNDLE-002", "quantity": 2}]
            }
            """.formatted(masterStore.getId());

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk());

        var bookStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), p1.getId()).orElseThrow();
        var penStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), p2.getId()).orElseThrow();
        
        assert bookStock.getQuantity() == 18;
        assert penStock.getQuantity() == 44;
    }

    @Test
    void testBundleWithExclusions() throws Exception {
        Product p1 = createProduct("BND-P5", "History Book", 120.0);
        Product p2 = createProduct("BND-P6", "Black Pen", 12.0);
        Product p3 = createProduct("BND-P7", "Eraser", 5.0);
        addStock(p1.getSku(), 30);
        addStock(p2.getSku(), 60);
        addStock(p3.getSku(), 100);

        String bundleJson = """
            {
                "sku": "BUNDLE-003",
                "name": "Complete Kit",
                "price": 130.0,
                "items": [
                    {"productSku": "BND-P5", "quantity": 1},
                    {"productSku": "BND-P6", "quantity": 2},
                    {"productSku": "BND-P7", "quantity": 1}
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bundleJson))
                .andExpect(status().isOk());

        String orderJson = """
            {
                "storeId": %d,
                "customerName": "Jane Doe",
                "items": [
                    {
                        "sku": "BUNDLE-003",
                        "quantity": 1,
                        "excludedProductSkus": ["BND-P7"]
                    }
                ]
            }
            """.formatted(masterStore.getId());

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk());

        var bookStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), p1.getId()).orElseThrow();
        var penStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), p2.getId()).orElseThrow();
        var eraserStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), p3.getId()).orElseThrow();
        
        assert bookStock.getQuantity() == 29;
        assert penStock.getQuantity() == 58;
        assert eraserStock.getQuantity() == 100;
    }

    @Test
    void testGetAllBundles() throws Exception {
        Product p1 = createProduct("BND-P8", "English Book", 110.0);
        addStock(p1.getSku(), 40);

        String bundleJson = """
            {
                "sku": "BUNDLE-004",
                "name": "English Kit",
                "price": 100.0,
                "items": [{"productSku": "BND-P8", "quantity": 1}]
            }
            """;

        mockMvc.perform(post("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bundleJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inventory/bundles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.sku=='BUNDLE-004')].name").value("English Kit"));
    }

    private Product createProduct(String sku, String name, double price) throws Exception {
        String json = """
            {
                "sku": "%s",
                "name": "%s",
                "basePrice": %.2f,
                "type": "BOOK"
            }
            """.formatted(sku, name, price);

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
}

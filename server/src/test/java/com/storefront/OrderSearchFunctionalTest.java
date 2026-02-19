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
public class OrderSearchFunctionalTest {

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
    void testSearchOrdersByCustomerName() throws Exception {
        Product product = createProduct("ORD-001", "Search Product", 50.0);
        addStock(product.getSku(), 100);

        createOrder("Alice Smith", "1111111111", product.getSku(), 2);
        createOrder("Bob Johnson", "2222222222", product.getSku(), 1);
        createOrder("Alice Brown", "3333333333", product.getSku(), 3);

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("customerName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].customer.name", containsInAnyOrder("Alice Smith", "Alice Brown")));
    }

    @Test
    void testSearchOrdersByCustomerPhone() throws Exception {
        Product product = createProduct("ORD-002", "Phone Search", 75.0);
        addStock(product.getSku(), 100);

        createOrder("Customer A", "9876543210", product.getSku(), 1);
        createOrder("Customer B", "9876549999", product.getSku(), 2);
        createOrder("Customer C", "1234567890", product.getSku(), 1);

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("customerPhone", "98765"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].customer.phone", containsInAnyOrder("9876543210", "9876549999")));
    }

    @Test
    void testOrdersSortedByNewestFirst() throws Exception {
        Product product = createProduct("ORD-003", "Sort Test", 60.0);
        addStock(product.getSku(), 100);

        createOrder("First Customer", "1111111111", product.getSku(), 1);
        Thread.sleep(100);
        createOrder("Second Customer", "2222222222", product.getSku(), 1);
        Thread.sleep(100);
        createOrder("Third Customer", "3333333333", product.getSku(), 1);

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customer.name").value("Third Customer"))
                .andExpect(jsonPath("$[1].customer.name").value("Second Customer"))
                .andExpect(jsonPath("$[2].customer.name").value("First Customer"));
    }

    @Test
    void testSearchWithNoResults() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("customerName", "NonExistentCustomer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testCaseInsensitiveSearch() throws Exception {
        Product product = createProduct("ORD-004", "Case Test", 45.0);
        addStock(product.getSku(), 50);

        createOrder("UPPERCASE NAME", "4444444444", product.getSku(), 1);

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("customerName", "uppercase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customer.name").value("UPPERCASE NAME"));
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

    private void createOrder(String customerName, String phone, String sku, int quantity) throws Exception {
        String orderJson = """
            {
                "storeId": %d,
                "customerName": "%s",
                "customerPhone": "%s",
                "items": [{"sku": "%s", "quantity": %d}]
            }
            """.formatted(masterStore.getId(), customerName, phone, sku, quantity);

        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk());
    }
}

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
public class ReconciliationFunctionalTest {

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
    void testReconciliationReportStructure() throws Exception {
        Store virtualStore = createStore("Pop-up Store");
        Product product = createProduct("REC-001", "Report Test", 100.0);
        addStock(product.getSku(), 100);
        allocateStock(virtualStore.getId(), product.getSku(), 50);

        createOrder(virtualStore.getId(), "Customer", "9999999999", product.getSku(), 10);

        mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/reconcile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Pop-up Store"))
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.totalItemsSold").value(10))
                .andExpect(jsonPath("$.soldItems").isArray())
                .andExpect(jsonPath("$.returnedItems").isArray())
                .andExpect(jsonPath("$.assignedAdmins").isArray());
    }

    @Test
    void testReconciliationHistory() throws Exception {
        Store virtualStore = createStore("Event Store");
        Product product = createProduct("REC-002", "History Test", 80.0);
        addStock(product.getSku(), 100);
        allocateStock(virtualStore.getId(), product.getSku(), 30);

        mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/reconcile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stores/" + virtualStore.getId() + "/reconciliation-history")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].storeName").value("Event Store"))
                .andExpect(jsonPath("$[0].reconciledBy").exists());
    }

    @Test
    void testMultipleReconciliationCycles() throws Exception {
        Store virtualStore = createStore("Multi Cycle Store");
        Product product = createProduct("REC-003", "Cycle Test", 60.0);
        addStock(product.getSku(), 200);

        allocateStock(virtualStore.getId(), product.getSku(), 50);
        createOrder(virtualStore.getId(), "Customer A", "1111111111", product.getSku(), 20);
        mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/reconcile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        allocateStock(virtualStore.getId(), product.getSku(), 40);
        createOrder(virtualStore.getId(), "Customer B", "2222222222", product.getSku(), 15);
        mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/reconcile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stores/" + virtualStore.getId() + "/reconciliation-history")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testReconciliationReturnsStockToMaster() throws Exception {
        Store virtualStore = createStore("Return Test Store");
        Product product = createProduct("REC-004", "Return Test", 90.0);
        addStock(product.getSku(), 100);
        
        var initialMasterStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), product.getId()).orElseThrow();
        int initialQty = initialMasterStock.getQuantity();

        allocateStock(virtualStore.getId(), product.getSku(), 50);
        createOrder(virtualStore.getId(), "Customer", "5555555555", product.getSku(), 10);

        mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/reconcile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var finalMasterStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), product.getId()).orElseThrow();
        assert finalMasterStock.getQuantity() == initialQty - 10;
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

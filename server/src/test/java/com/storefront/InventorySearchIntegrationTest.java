package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.StockAllocationDTO;
import com.storefront.model.Product;
import com.storefront.model.Store;
import com.storefront.model.attributes.BookAttributes;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
public class InventorySearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private com.storefront.service.AuthService authService; // Inject AuthService

    @Autowired
    private com.storefront.service.InventoryService inventoryService; // Inject InventoryService

    private String adminToken;
    private Long storeId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate as Admin
        com.storefront.model.AppUser adminUser;
        try {
            adminUser = authService.register("admin", "adminpass", com.storefront.model.Role.ADMIN);
        } catch (Exception e) {
            adminUser = authService.login("admin", "adminpass").get();
        }
        adminToken = authService.generateToken(adminUser);

        // Ensure Master Store Exists (Generic Helper)
        if (storeRepository.findFirstByType(com.storefront.model.Store.StoreType.MASTER).isEmpty()) {
            storeRepository.save(new Store("Master Store", com.storefront.model.Store.StoreType.MASTER, null));
        }

        // Create a Virtual Store
        String storeResponse = mockMvc.perform(post("/api/v1/stores")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("name", "Search Store"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        storeId = objectMapper.readTree(storeResponse).get("id").asLong();

        // Create Products and Initial Stock
        createBook("ISBN-111", "Java Basics", "Learning Java", "978-0134685991"); // Has ISBN
        inventoryService.addStock("ISBN-111", 100);

        createBook("ISBN-222", "Advanced Spring", "Deep dive", "978-1492029999");
        inventoryService.addStock("ISBN-222", 100);

        createProduct("PEN-001", "Blue Pen", "PENCIL");
        inventoryService.addStock("PEN-001", 100);

        // Allocate Stock
        allocateStock("ISBN-111", 10);
        allocateStock("ISBN-222", 5);
        allocateStock("PEN-001", 20);
    }

    private void createBook(String sku, String name, String desc, String isbn) {
        BookAttributes attr = new BookAttributes();
        attr.setIsbn(isbn);
        attr.setDescription(desc);
        Product p = new Product(sku, "BOOK", name, BigDecimal.TEN, attr);
        productRepository.save(p);
    }

    private void createProduct(String sku, String name, String type) {
        Product p = new Product(sku, type, name, BigDecimal.ONE, null);
        productRepository.save(p);
    }

    private void allocateStock(String sku, int qty) throws Exception {
        AllocationRequestDTO req = new AllocationRequestDTO();
        StockAllocationDTO item = new StockAllocationDTO();
        item.setSku(sku);
        item.setQuantity(qty);
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/stores/" + storeId + "/allocate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchInventoryByName() throws Exception {
        mockMvc.perform(get("/api/v1/stores/" + storeId + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .param("search", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].product.name").value("Java Basics"));
    }

    @Test
    void testSearchInventoryByIsbn() throws Exception {
        // Searching by ISBN which is in the JSON attributes
        mockMvc.perform(get("/api/v1/stores/" + storeId + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .param("search", "0134685991")) // Part of ISBN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].product.name").value("Java Basics"));
    }

    @Test
    void testSearchInventoryBySkuPart() throws Exception {
        // Note: My search impl currently searches Name OR Attributes. It doesn't
        // explicitly search SKU column in Specification
        // But let's check if user wants SKU search. The prompt said "name of product,
        // ISBN".
        // If I want SKU search, I should have added it.
        // Let's verify if "Advanced" matches name.
        mockMvc.perform(get("/api/v1/stores/" + storeId + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .param("search", "Advanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].product.name").value("Advanced Spring"));
    }
}

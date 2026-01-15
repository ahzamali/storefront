package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.StockAllocationDTO;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.StockLevelRepository;
import com.storefront.repository.StoreRepository;
import com.storefront.service.AuthService;
import com.storefront.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_conc;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
public class ConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private StockLevelRepository stockLevelRepository;
    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private String adminToken;
    private Store virtualStore;
    private Long productId;

    @BeforeEach
    void setup() {
        if (storeRepository.findFirstByType(Store.StoreType.MASTER).isEmpty()) {
            storeRepository.save(new Store("Master Warehouse", Store.StoreType.MASTER, null));
        }

        // Admin
        if (authService.login("admin_conc", "pass").isEmpty()) {
            var admin = authService.register("admin_conc", "pass", Role.SUPER_ADMIN);
            adminToken = authService.generateToken(admin);
        } else {
            adminToken = authService.generateToken(authService.login("admin_conc", "pass").get());
        }

        // Product
        var prod = inventoryService.createProduct(
                new com.storefront.model.Product("SKU-CONC-1", "ALL", "Conc", new BigDecimal("10"), null));
        productId = prod.getId();

        // Stock (Set 100)
        inventoryService.addStock("SKU-CONC-1", 100);

        // Virtual Store
        var store = new Store("Virtual Conc", Store.StoreType.VIRTUAL, null);
        virtualStore = storeRepository.save(store);
    }

    @Test
    void testConcurrentAllocation() throws Exception {
        // Try to allocate 10 items, 10 times concurrently.
        // Total items in system should remain 100.
        // Master stock should be 100 - (10 * successful_transactions)

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();

        AllocationRequestDTO request = new AllocationRequestDTO();
        StockAllocationDTO item = new StockAllocationDTO();
        item.setSku("SKU-CONC-1");
        item.setQuantity(10);
        request.setItems(List.of(item));
        String jsonRequest = objectMapper.writeValueAsString(request);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/v1/stores/" + virtualStore.getId() + "/allocate")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignore failures (timeouts)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);

        // Verify Stock Consistency
        transactionTemplate.execute(status -> {
            var masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER).get();
            int masterQty = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), productId).get()
                    .getQuantity();
            int virtualQty = stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), productId)
                    .map(sl -> sl.getQuantity()).orElse(0);

            int totalStock = masterQty + virtualQty;

            System.out.println("Success count: " + successCount.get());
            System.out.println("Master Qty: " + masterQty);
            System.out.println("Virtual Qty: " + virtualQty);
            System.out.println("Total Stock: " + totalStock);

            assertEquals(100, totalStock, "Total stock must be conserved (no lost updates)");
            assertEquals(100 - (successCount.get() * 10), masterQty, "Master stock must match successful allocations");
            return null;
        });

        // We expect high success rate with extended timeout, but assertions on
        // consistency are paramount.
        // Ideally > 0
        if (successCount.get() == 0) {
            throw new RuntimeException("Zero successful allocations - check configuration");
        }
    }
}

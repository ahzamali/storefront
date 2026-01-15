package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.*;
import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.StockLevelRepository;
import com.storefront.repository.StoreRepository;
import com.storefront.service.AuthService;
import com.storefront.service.InventoryService;
import com.storefront.service.StoreService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb_order;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
public class OrderIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private AuthService authService;
        @Autowired
        private StoreRepository storeRepository;
        @Autowired
        private StockLevelRepository stockLevelRepository;
        @Autowired
        private InventoryService inventoryService;
        @Autowired
        private StoreService storeService;

        private String token;
        private AppUser user;
        private Store virtualStore;

        @BeforeEach
        void setup() {
                // Setup Master and Products
                Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                                .orElseGet(() -> storeRepository
                                                .save(new Store("Master", Store.StoreType.MASTER, null)));

                inventoryService.createProduct(
                                new com.storefront.model.Product("SKU-B1", "BOOK", "Book 1", new BigDecimal("10"),
                                                null));
                inventoryService.createProduct(
                                new com.storefront.model.Product("SKU-P1", "STATIONERY", "Pen 1", new BigDecimal("5"),
                                                null));
                inventoryService.addStock("SKU-B1", 100);
                inventoryService.addStock("SKU-P1", 100);

                // Bundle: 1 Book + 1 Pen = Total 15 (Deal: 12)
                BundleDTO bundle = new BundleDTO();
                bundle.setSku("BUN-1");
                bundle.setName("Set");
                bundle.setPrice(new BigDecimal("12"));
                BundleDTO.BundleItemDTO bi1 = new BundleDTO.BundleItemDTO();
                bi1.setProductSku("SKU-B1");
                bi1.setQuantity(1);
                BundleDTO.BundleItemDTO bi2 = new BundleDTO.BundleItemDTO();
                bi2.setProductSku("SKU-P1");
                bi2.setQuantity(1);
                bundle.setItems(List.of(bi1, bi2));
                inventoryService.createBundle(bundle);

                // Create Admin/User
                try {
                        user = authService.register("employee", "pass", Role.SUPER_ADMIN);
                } catch (Exception e) {
                        user = authService.login("employee", "pass").get();
                        if (user.getRole() != Role.SUPER_ADMIN) {
                                user.setRole(Role.SUPER_ADMIN);
                                // Need to save. OrderIntegrationTest doesn't inject AppUserRepository.
                                // But usually H2 resets between tests?
                                // OrderIntegrationTest uses @Transactional? Yes.
                                // But wait, setup() runs before each test.
                                // If register fails, it means user exists.
                        }
                }
                token = authService.generateToken(user);

                // Create Virtual Store
                virtualStore = storeRepository.save(new Store("PopUp", Store.StoreType.VIRTUAL, user));

                // Allocate Stock to Virtual Store (20 Books, 20 Pens)
                AllocationRequestDTO alloc = new AllocationRequestDTO();
                StockAllocationDTO item1 = new StockAllocationDTO();
                item1.setSku("SKU-B1");
                item1.setQuantity(20);
                StockAllocationDTO item2 = new StockAllocationDTO();
                item2.setSku("SKU-P1");
                item2.setQuantity(20);
                alloc.setItems(List.of(item1, item2));
                storeService.allocateStock(virtualStore.getId(), alloc, user);
        }

        @Test
        void testOrderFlow() throws Exception {
                // 1. Buy 2 Books
                OrderRequestDTO order1 = new OrderRequestDTO();
                order1.setStoreId(virtualStore.getId());
                OrderItemRequestDTO item1 = new OrderItemRequestDTO();
                item1.setSku("SKU-B1");
                item1.setQuantity(2);
                order1.setItems(List.of(item1));

                mockMvc.perform(post("/api/v1/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(order1)))
                                .andExpect(status().isOk());

                // Verify Stock: Books 20 -> 18
                var bookId = inventoryService.getAllProducts().stream().filter(p -> p.getSku().equals("SKU-B1"))
                                .findFirst()
                                .get().getId();
                assertEquals(18,
                                stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), bookId).get()
                                                .getQuantity());

                // 2. Buy 1 Bundle (Book + Pen)
                OrderRequestDTO order2 = new OrderRequestDTO();
                order2.setStoreId(virtualStore.getId());
                OrderItemRequestDTO item2 = new OrderItemRequestDTO();
                item2.setSku("BUN-1");
                item2.setQuantity(1);
                order2.setItems(List.of(item2));

                mockMvc.perform(post("/api/v1/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(order2)))
                                .andExpect(status().isOk());

                // Verify Stock: Books 18 -> 17, Pens 20 -> 19
                var penId = inventoryService.getAllProducts().stream().filter(p -> p.getSku().equals("SKU-P1"))
                                .findFirst()
                                .get().getId();
                assertEquals(17,
                                stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), bookId).get()
                                                .getQuantity());
                assertEquals(19,
                                stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), penId).get()
                                                .getQuantity());

                // 3. Buy 1 Bundle with Exclusion (Exclude Pen)
                OrderRequestDTO order3 = new OrderRequestDTO();
                order3.setStoreId(virtualStore.getId());
                OrderItemRequestDTO item3 = new OrderItemRequestDTO();
                item3.setSku("BUN-1");
                item3.setQuantity(1);
                item3.setExcludedProductSkus(List.of("SKU-P1"));
                order3.setItems(List.of(item3));

                mockMvc.perform(post("/api/v1/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(order3)))
                                .andExpect(status().isOk());

                // Verify Stock: Books 17 -> 16, Pens 19 -> 19 (Unchanged)
                assertEquals(16,
                                stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), bookId).get()
                                                .getQuantity());
                assertEquals(19,
                                stockLevelRepository.findByStoreIdAndProductId(virtualStore.getId(), penId).get()
                                                .getQuantity());
        }

        @Test
        void testOrder_InsufficientStock() throws Exception {
                OrderRequestDTO order = new OrderRequestDTO();
                order.setStoreId(virtualStore.getId());
                OrderItemRequestDTO item = new OrderItemRequestDTO();
                item.setSku("SKU-B1");
                item.setQuantity(100); // Only 20 available
                order.setItems(List.of(item));

                mockMvc.perform(post("/api/v1/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(order)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOrder_InvalidStore() throws Exception {
                OrderRequestDTO order = new OrderRequestDTO();
                order.setStoreId(999999L); // Non-existent
                OrderItemRequestDTO item = new OrderItemRequestDTO();
                item.setSku("SKU-B1");
                item.setQuantity(1);
                order.setItems(List.of(item));

                mockMvc.perform(post("/api/v1/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(order)))
                                .andExpect(status().isBadRequest());
        }
}

package com.storefront;

import com.storefront.model.*;
import com.storefront.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CoreEntityIntegrationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private BundleRepository bundleRepository;
    @Autowired
    private BundleItemRepository bundleItemRepository;
    @Autowired
    private StockLevelRepository stockLevelRepository;
    @Autowired
    private CustomerOrderRepository customerOrderRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void testCoreEntitiesLifecycle() {
        // 1. Create AppUser (Owner)
        AppUser admin = new AppUser("admin_test", "hash", Role.ADMIN);
        admin = appUserRepository.save(admin);
        assertNotNull(admin.getId());

        // 2. Create Store
        Store masterStore = new Store("Master Warehouse", Store.StoreType.MASTER, admin);
        masterStore = storeRepository.save(masterStore);
        assertNotNull(masterStore.getId());

        // 3. Create Product
        Product book = new Product("ISBN-123", "BOOK", "Physics 101", new BigDecimal("50.00"), "{}");
        book = productRepository.save(book);
        assertNotNull(book.getId());

        // 4. Create Bundle
        Bundle bundle = new Bundle("B-001", "Grade 10 Set", "All books for grade 10", new BigDecimal("100.00"));
        bundle = bundleRepository.save(bundle);
        assertNotNull(bundle.getId());

        // 5. Add Item to Bundle
        BundleItem bundleItem = new BundleItem(bundle, book, 1);
        bundleItem = bundleItemRepository.save(bundleItem);
        assertNotNull(bundleItem.getBundle());

        // 6. Add Stock
        StockLevel stock = new StockLevel(masterStore, book, 100);
        stock = stockLevelRepository.save(stock);
        assertEquals(100, stock.getQuantity());

        // 7. Verify Stock Retrieval
        StockLevel fetchedStock = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), book.getId())
                .orElseThrow();
        assertEquals(100, fetchedStock.getQuantity());

        // 8. Create Order
        CustomerOrder order = new CustomerOrder(masterStore, admin, new BigDecimal("50.00"),
                CustomerOrder.OrderStatus.COMPLETED);

        // Add one order line
        OrderLine line = new OrderLine(book, null, new BigDecimal("50.00"), false, 1);
        order.addOrderLine(line);

        order = customerOrderRepository.save(order);
        assertNotNull(order.getId());
        assertEquals(1, order.getOrderLines().size());

        // 9. Cleanup implicitly handled by @Transactional rollbacks or H2 in-memory
    }
}

package com.storefront;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.AppUserRepository;
import com.storefront.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserStoreAssignmentTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    void testUserWithMultipleStores() {
        // Create user
        AppUser user = new AppUser("multi_store_user", "hashedPassword", Role.STORE_ADMIN);

        // Create and assign multiple stores
        Store store1 = new Store("Store 1", Store.StoreType.VIRTUAL, null);
        Store store2 = new Store("Store 2", Store.StoreType.VIRTUAL, null);
        store1 = storeRepository.save(store1);
        store2 = storeRepository.save(store2);

        Set<Store> stores = new HashSet<>();
        stores.add(store1);
        stores.add(store2);
        user.setStores(stores);

        user = userRepository.save(user);

        // Verify
        assertNotNull(user.getId());
        assertEquals(2, user.getStores().size());
        assertTrue(user.getStores().contains(store1));
        assertTrue(user.getStores().contains(store2));
    }

    @Test
    void testUserWithSingleStore() {
        // Create user with single store
        AppUser user = new AppUser("single_store_user", "hashedPassword", Role.EMPLOYEE);

        Store store = new Store("Single Store", Store.StoreType.VIRTUAL, null);
        store = storeRepository.save(store);

        user.addStore(store);
        user = userRepository.save(user);

        // Verify
        assertNotNull(user.getId());
        assertEquals(1, user.getStores().size());
        assertTrue(user.getStores().contains(store));
    }

    @Test
    void testUserWithNoStores() {
        // Super Admin with no specific store assignments
        AppUser user = new AppUser("superadmin_test", "hashedPassword", Role.SUPER_ADMIN);
        user = userRepository.save(user);

        // Verify
        assertNotNull(user.getId());
        assertEquals(0, user.getStores().size());
    }

    @Test
    void testAddAndRemoveStores() {
        // Create user
        AppUser user = new AppUser("dynamic_user", "hashedPassword", Role.STORE_ADMIN);
        user = userRepository.save(user);

        // Add first store
        Store store1 = storeRepository.save(new Store("Store A", Store.StoreType.VIRTUAL, null));
        user.addStore(store1);
        user = userRepository.save(user);
        assertEquals(1, user.getStores().size());

        // Add second store
        Store store2 = storeRepository.save(new Store("Store B", Store.StoreType.VIRTUAL, null));
        user.addStore(store2);
        user = userRepository.save(user);
        assertEquals(2, user.getStores().size());

        // Remove one store
        user.removeStore(store1);
        user = userRepository.save(user);
        assertEquals(1, user.getStores().size());
        assertFalse(user.getStores().contains(store1));
        assertTrue(user.getStores().contains(store2));
    }
}

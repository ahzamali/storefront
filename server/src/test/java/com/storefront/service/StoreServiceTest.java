package com.storefront.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.StockAllocationDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BundleRepository bundleRepository;
    @Mock
    private BundleItemRepository bundleItemRepository;
    @Mock
    private StockLevelRepository stockLevelRepository;
    @Mock
    private InventoryTransferRepository transferRepository;
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private ReconciliationLogRepository reconciliationLogRepository;

    private StoreService storeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        storeService = new StoreService(storeRepository, productRepository, bundleRepository,
                bundleItemRepository, stockLevelRepository, transferRepository, orderRepository,
                userRepository, reconciliationLogRepository, objectMapper);
    }

    @Test
    void createStore_SavesStore() {
        AppUser owner = new AppUser("owner", "pass", Role.SUPER_ADMIN);
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, owner);
        when(storeRepository.save(any(Store.class))).thenReturn(store);

        Store result = storeService.createStore("Store1", Store.StoreType.VIRTUAL, owner);

        assertNotNull(result);
        assertEquals("Store1", result.getName());
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void allocateStock_UserWithoutAccess_ThrowsException() {
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);
        user.setStores(Set.of());
        Store masterStore = new Store("Master", Store.StoreType.MASTER, null);
        Store targetStore = new Store("Target", Store.StoreType.VIRTUAL, null);

        AllocationRequestDTO request = new AllocationRequestDTO();
        StockAllocationDTO item = new StockAllocationDTO();
        item.setSku("SKU1");
        item.setQuantity(5);
        request.setItems(List.of(item));

        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.of(masterStore));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(targetStore));

        assertThrows(AccessDeniedException.class, () -> storeService.allocateStock(1L, request, user));
    }

    @Test
    void allocateStock_SuperAdmin_AllowsAccess() {
        AppUser superAdmin = new AppUser("admin", "pass", Role.SUPER_ADMIN);
        Store masterStore = new Store("Master", Store.StoreType.MASTER, null);
        Store targetStore = new Store("Target", Store.StoreType.VIRTUAL, null);
        Product product = new Product();
        product.setSku("SKU1");
        StockLevel masterStock = new StockLevel(masterStore, product, 100);

        AllocationRequestDTO request = new AllocationRequestDTO();
        StockAllocationDTO item = new StockAllocationDTO();
        item.setSku("SKU1");
        item.setQuantity(5);
        request.setItems(List.of(item));

        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.of(masterStore));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(targetStore));
        when(productRepository.findBySku("SKU1")).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByStoreIdAndProductId(any(), any()))
                .thenReturn(Optional.of(masterStock))
                .thenReturn(Optional.empty());
        when(stockLevelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> storeService.allocateStock(2L, request, superAdmin));
    }

    @Test
    void getAllStores_ReturnsAllStores() {
        List<Store> stores = List.of(new Store(), new Store());
        when(storeRepository.findAll()).thenReturn(stores);

        List<Store> result = storeService.getAllStores();

        assertEquals(2, result.size());
        verify(storeRepository).findAll();
    }
}

package com.storefront.service;

import com.storefront.dto.OrderItemRequestDTO;
import com.storefront.dto.OrderRequestDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceValidationTest {

    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private OrderLineRepository orderLineRepository;
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
    private CustomerRepository customerRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderLineRepository, storeRepository,
                productRepository, bundleRepository, bundleItemRepository, stockLevelRepository, customerRepository);
    }

    @Test
    void createOrder_ZeroQuantity_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku("SKU1");
        item.setQuantity(0);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThrows(Exception.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void createOrder_NegativeQuantity_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku("SKU1");
        item.setQuantity(-5);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThrows(Exception.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void createOrder_EmptyItems_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        request.setItems(List.of());

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThrows(Exception.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void createOrder_NullSku_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku(null);
        item.setQuantity(1);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThrows(Exception.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void createOrder_InsufficientStock_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);
        Product product = new Product();
        product.setSku("SKU1");
        product.setBasePrice(BigDecimal.TEN);
        StockLevel stockLevel = new StockLevel(store, product, 5);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku("SKU1");
        item.setQuantity(10);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findBySku("SKU1")).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByStoreIdAndProductId(any(), any())).thenReturn(Optional.of(stockLevel));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request, user));
    }
}

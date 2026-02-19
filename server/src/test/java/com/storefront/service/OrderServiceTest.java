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
class OrderServiceTest {

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
    void createOrder_WithProduct_CreatesOrderAndDecrementStock() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);
        Product product = new Product();
        product.setSku("SKU1");
        product.setBasePrice(BigDecimal.TEN);
        StockLevel stockLevel = new StockLevel(store, product, 10);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku("SKU1");
        item.setQuantity(2);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findBySku("SKU1")).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByStoreIdAndProductId(any(), any())).thenReturn(Optional.of(stockLevel));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerOrder result = orderService.createOrder(request, user);

        assertNotNull(result);
        assertEquals(store, result.getStore());
    }

    @Test
    void createOrder_StoreNotFound_ThrowsException() {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(999L);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        AppUser user = new AppUser("user", "pass", Role.EMPLOYEE);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setStoreId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setSku("INVALID");
        item.setQuantity(1);
        request.setItems(List.of(item));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findBySku("INVALID")).thenReturn(Optional.empty());
        when(bundleRepository.findBySku("INVALID")).thenReturn(Optional.empty());
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request, user));
    }

    @Test
    void searchOrders_ReturnsOrders() {
        List<CustomerOrder> orders = List.of(new CustomerOrder(), new CustomerOrder());
        when(orderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), 
                any(org.springframework.data.domain.Sort.class))).thenReturn(orders);

        List<CustomerOrder> result = orderService.searchOrders(null, null, null);

        assertEquals(2, result.size());
    }
}

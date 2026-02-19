package com.storefront.service;

import com.storefront.dto.BundleDTO;
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
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BundleRepository bundleRepository;
    @Mock
    private BundleItemRepository bundleItemRepository;
    @Mock
    private StockLevelRepository stockLevelRepository;
    @Mock
    private StoreRepository storeRepository;

    private InventoryService inventoryService;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(); // Use real instance instead of mock
        inventoryService = new InventoryService(productRepository, bundleRepository, bundleItemRepository,
                stockLevelRepository, storeRepository, bookService);
    }

    @Test
    void createProduct_SavesProduct() {
        Product product = new Product();
        product.setSku("SKU1");
        when(productRepository.save(product)).thenReturn(product);

        Product result = inventoryService.createProduct(product);

        assertNotNull(result);
        assertEquals("SKU1", result.getSku());
        verify(productRepository).save(product);
    }

    @Test
    void getAllProducts_ReturnsAllProducts() {
        List<Product> products = List.of(new Product(), new Product());
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = inventoryService.getAllProducts();

        assertEquals(2, result.size());
        verify(productRepository).findAll();
    }

    @Test
    void createBundle_WithValidProducts_CreatesBundle() {
        Product product = new Product();
        product.setSku("SKU1");
        Bundle bundle = new Bundle("BUNDLE1", "Test Bundle", "Desc", BigDecimal.TEN);

        BundleDTO.BundleItemDTO itemDTO = new BundleDTO.BundleItemDTO();
        itemDTO.setProductSku("SKU1");
        itemDTO.setQuantity(2);

        BundleDTO dto = new BundleDTO();
        dto.setSku("BUNDLE1");
        dto.setName("Test Bundle");
        dto.setDescription("Desc");
        dto.setPrice(BigDecimal.TEN);
        dto.setItems(List.of(itemDTO));

        when(bundleRepository.save(any(Bundle.class))).thenReturn(bundle);
        when(productRepository.findBySku("SKU1")).thenReturn(Optional.of(product));

        Bundle result = inventoryService.createBundle(dto);

        assertNotNull(result);
        verify(bundleRepository).save(any(Bundle.class));
        verify(bundleItemRepository).save(any(BundleItem.class));
    }

    @Test
    void createBundle_ProductNotFound_ThrowsException() {
        BundleDTO.BundleItemDTO itemDTO = new BundleDTO.BundleItemDTO();
        itemDTO.setProductSku("INVALID");
        itemDTO.setQuantity(1);

        BundleDTO dto = new BundleDTO();
        dto.setSku("BUNDLE1");
        dto.setItems(List.of(itemDTO));

        when(bundleRepository.save(any(Bundle.class))).thenReturn(new Bundle());
        when(productRepository.findBySku("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> inventoryService.createBundle(dto));
    }

    @Test
    void addStock_ValidSku_AddsStock() {
        Product product = new Product();
        product.setSku("SKU1");
        Store masterStore = new Store("Master", Store.StoreType.MASTER, null);
        StockLevel stockLevel = new StockLevel(masterStore, product, 10);

        when(productRepository.findBySku("SKU1")).thenReturn(Optional.of(product));
        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.of(masterStore));
        when(stockLevelRepository.findByStoreIdAndProductId(any(), any())).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        StockLevel result = inventoryService.addStock("SKU1", 5);

        assertEquals(15, result.getQuantity());
        verify(stockLevelRepository).save(stockLevel);
    }

    @Test
    void addStock_ProductNotFound_ThrowsException() {
        when(productRepository.findBySku("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> inventoryService.addStock("INVALID", 5));
    }
}

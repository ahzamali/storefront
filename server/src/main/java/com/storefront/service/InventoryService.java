package com.storefront.service;

import com.storefront.dto.BundleDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;
    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StoreRepository storeRepository;

    public InventoryService(ProductRepository productRepository, BundleRepository bundleRepository,
            BundleItemRepository bundleItemRepository, StockLevelRepository stockLevelRepository,
            StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.storeRepository = storeRepository;
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Bundle createBundle(BundleDTO dto) {
        Bundle bundle = new Bundle(dto.getSku(), dto.getName(), dto.getDescription(), dto.getPrice());
        bundle = bundleRepository.save(bundle);

        for (BundleDTO.BundleItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findBySku(itemDTO.getProductSku())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDTO.getProductSku()));

            BundleItem bundleItem = new BundleItem(bundle, product, itemDTO.getQuantity());
            bundleItemRepository.save(bundleItem);
        }
        return bundle;
    }

    public StockLevel addStock(String sku, int quantity) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found initialized"));

        StockLevel stockLevel = stockLevelRepository.findByStoreIdAndProductId(masterStore.getId(), product.getId())
                .orElse(new StockLevel(masterStore, product, 0));

        stockLevel.setQuantity(stockLevel.getQuantity() + quantity);
        return stockLevelRepository.save(stockLevel);
    }

    public List<com.storefront.dto.ProductStockDTO> getInventoryView() {
        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found initialized"));

        List<Product> products = productRepository.findAll();
        List<StockLevel> stocks = stockLevelRepository.findByStoreId(masterStore.getId());

        // Map product ID to stock quantity for efficient lookup
        java.util.Map<Long, Integer> stockMap = stocks.stream()
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.getProduct().getId(),
                        StockLevel::getQuantity));

        return products.stream()
                .map(p -> new com.storefront.dto.ProductStockDTO(
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getType(),
                        p.getBasePrice(),
                        stockMap.getOrDefault(p.getId(), 0)))
                .collect(java.util.stream.Collectors.toList());
    }
}

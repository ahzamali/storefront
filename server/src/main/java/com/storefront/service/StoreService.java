package com.storefront.service;

import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.StockAllocationDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final InventoryTransferRepository transferRepository;

    public StoreService(StoreRepository storeRepository, ProductRepository productRepository,
            BundleRepository bundleRepository, BundleItemRepository bundleItemRepository,
            StockLevelRepository stockLevelRepository, InventoryTransferRepository transferRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.transferRepository = transferRepository;
    }

    public Store createStore(String name, Store.StoreType type, AppUser owner) {
        return storeRepository.save(new Store(name, type, owner));
    }

    public void allocateStock(Long targetStoreId, AllocationRequestDTO request, AppUser currentUser) {
        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found"));
        Store targetStore = storeRepository.findById(targetStoreId)
                .orElseThrow(() -> new IllegalArgumentException("Target Store not found"));

        for (StockAllocationDTO item : request.getItems()) {
            Optional<Product> productOpt = productRepository.findBySku(item.getSku());

            if (productOpt.isPresent()) {
                // It's a single product
                moveProduct(masterStore, targetStore, productOpt.get(), item.getQuantity(), currentUser);
            } else {
                // Check if it's a bundle
                Bundle bundle = bundleRepository.findBySku(item.getSku())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "SKU not found (Product or Bundle): " + item.getSku()));

                // Explode Bundle
                var bundleItems = bundleItemRepository.findByBundleId(bundle.getId());
                for (BundleItem bi : bundleItems) {
                    int totalQty = bi.getQuantity() * item.getQuantity();
                    moveProduct(masterStore, targetStore, bi.getProduct(), totalQty, currentUser);
                }
            }
        }
    }

    private void moveProduct(Store from, Store to, Product product, int quantity, AppUser user) {
        StockLevel fromStock = stockLevelRepository.findByStoreIdAndProductId(from.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not available in source store: " + product.getSku()));

        if (fromStock.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for " + product.getSku() + ". Available: " + fromStock.getQuantity());
        }

        // Decrement Source
        fromStock.setQuantity(fromStock.getQuantity() - quantity);
        stockLevelRepository.save(fromStock);

        // Increment Target
        StockLevel toStock = stockLevelRepository.findByStoreIdAndProductId(to.getId(), product.getId())
                .orElse(new StockLevel(to, product, 0));
        toStock.setQuantity(toStock.getQuantity() + quantity);
        stockLevelRepository.save(toStock);

        // Record Transfer
        InventoryTransfer transfer = new InventoryTransfer(from, to, product, quantity, user);
        transferRepository.save(transfer);
    }

    public void reconcileStore(Long storeId, AppUser currentUser) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found"));

        var stockLevels = stockLevelRepository.findByStoreId(storeId);
        for (StockLevel sl : stockLevels) {
            if (sl.getQuantity() > 0) {
                moveProduct(store, masterStore, sl.getProduct(), sl.getQuantity(), currentUser);
            }
        }
    }

    public java.util.List<Store> getAllStores() {
        return storeRepository.findAll();
    }
}

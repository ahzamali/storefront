package com.storefront.service;

import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.StockAllocationDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final CustomerOrderRepository orderRepository;
    private final AppUserRepository userRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final ObjectMapper objectMapper;

    public StoreService(StoreRepository storeRepository, ProductRepository productRepository,
            BundleRepository bundleRepository, BundleItemRepository bundleItemRepository,
            StockLevelRepository stockLevelRepository, InventoryTransferRepository transferRepository,
            CustomerOrderRepository orderRepository, AppUserRepository userRepository,
            ReconciliationLogRepository reconciliationLogRepository, ObjectMapper objectMapper) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.transferRepository = transferRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.reconciliationLogRepository = reconciliationLogRepository;
        this.objectMapper = objectMapper;
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

    public void returnStock(Long fromStoreId, AllocationRequestDTO request, AppUser currentUser) {
        Store fromStore = storeRepository.findById(fromStoreId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found"));

        for (StockAllocationDTO item : request.getItems()) {
            Optional<Product> productOpt = productRepository.findBySku(item.getSku());

            if (productOpt.isPresent()) {
                moveProduct(fromStore, masterStore, productOpt.get(), item.getQuantity(), currentUser);
            } else {
                Bundle bundle = bundleRepository.findBySku(item.getSku())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "SKU not found for return: " + item.getSku()));

                var bundleItems = bundleItemRepository.findByBundleId(bundle.getId());
                for (BundleItem bi : bundleItems) {
                    int totalQty = bi.getQuantity() * item.getQuantity();
                    moveProduct(fromStore, masterStore, bi.getProduct(), totalQty, currentUser);
                }
            }
        }
    }

    public com.storefront.dto.ReconciliationReportDTO reconcileStore(Long storeId, boolean returnStock,
            AppUser currentUser) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Store masterStore = storeRepository.findFirstByType(Store.StoreType.MASTER)
                .orElseThrow(() -> new IllegalStateException("Master Store not found"));

        // 1. Calculate Sales Revenue & Items Sold from unreconciled orders
        List<CustomerOrder> orders = orderRepository.findByStoreIdAndReconciledFalse(storeId);

        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        Map<String, com.storefront.dto.ReconciliationReportDTO.ItemSales> soldItemsMap = new HashMap<>();

        for (CustomerOrder order : orders) {
            totalRevenue = totalRevenue.add(order.getTotalAmount());
            order.setReconciled(true);

            for (com.storefront.model.OrderLine line : order.getOrderLines()) {
                String sku = line.getProductSku();
                String name = line.getProductName();
                int qty = line.getQuantity();
                java.math.BigDecimal lineTotal = line.getPrice().multiply(new java.math.BigDecimal(qty));

                soldItemsMap.compute(sku, (k, v) -> {
                    if (v == null) {
                        return new com.storefront.dto.ReconciliationReportDTO.ItemSales(sku, name, qty, lineTotal);
                    } else {
                        return new com.storefront.dto.ReconciliationReportDTO.ItemSales(sku, name,
                                v.getQuantity() + qty, v.getTotal().add(lineTotal));
                    }
                });
            }
            orderRepository.save(order);
        }

        int totalItemsSold = orders.stream()
                .mapToInt(o -> o.getOrderLines().stream().mapToInt(l -> l.getQuantity()).sum()).sum();
        List<com.storefront.dto.ReconciliationReportDTO.ItemSales> soldItemsList = new ArrayList<>(
                soldItemsMap.values());

        // 2. Identify Stock to Return
        List<com.storefront.dto.ReconciliationReportDTO.ReturnedItem> returnedItems = new ArrayList<>();
        if (returnStock) {
            var stockLevels = stockLevelRepository.findByStoreId(storeId);
            for (StockLevel sl : stockLevels) {
                if (sl.getQuantity() > 0) {
                    returnedItems.add(new com.storefront.dto.ReconciliationReportDTO.ReturnedItem(
                            sl.getProduct().getSku(),
                            sl.getProduct().getName(),
                            sl.getQuantity()));
                    // Move back to master
                    moveProduct(store, masterStore, sl.getProduct(), sl.getQuantity(), currentUser);
                }
            }
        }

        // 3. Get Assigned Admins
        List<String> assignedAdmins = userRepository.findByStores_Id(storeId).stream()
                .map(AppUser::getUsername)
                .collect(java.util.stream.Collectors.toList());

        com.storefront.dto.ReconciliationReportDTO reportDTO = new com.storefront.dto.ReconciliationReportDTO(
                store.getId(),
                store.getName(),
                totalRevenue,
                totalItemsSold,
                soldItemsList,
                returnedItems,
                assignedAdmins);

        // 4. Save Persistent Log
        try {
            String detailsJson = objectMapper.writeValueAsString(reportDTO);
            ReconciliationLog log = new ReconciliationLog(
                    store,
                    currentUser,
                    totalRevenue,
                    totalItemsSold,
                    returnStock,
                    detailsJson);
            reconciliationLogRepository.save(log);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing reconciliation report", e);
        }

        return reportDTO;
    }

    public List<ReconciliationLog> getReconciliationHistory(Long storeId) {
        return reconciliationLogRepository.findByStoreIdOrderByReconciledAtDesc(storeId);
    }

    public java.util.List<Store> getAllStores() {
        return storeRepository.findAll();
    }
}

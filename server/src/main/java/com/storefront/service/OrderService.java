package com.storefront.service;

import com.storefront.dto.OrderItemRequestDTO;
import com.storefront.dto.OrderRequestDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final StockLevelRepository stockLevelRepository;

    public OrderService(CustomerOrderRepository orderRepository, OrderLineRepository orderLineRepository,
            StoreRepository storeRepository, ProductRepository productRepository,
            BundleRepository bundleRepository, BundleItemRepository bundleItemRepository,
            StockLevelRepository stockLevelRepository) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.stockLevelRepository = stockLevelRepository;
    }

    public CustomerOrder createOrder(OrderRequestDTO request, AppUser currentUser) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        CustomerOrder order = new CustomerOrder();
        order.setStore(store);
        order.setProcessedBy(currentUser);
        order.setStatus(CustomerOrder.OrderStatus.COMPLETED); // Instant completion for now
        order.setTotalAmount(BigDecimal.ZERO);

        // We need to save order first? No, cascade usually. But simple way: save order
        // first.
        order = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderLine> lines = new ArrayList<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            Optional<Product> productOpt = productRepository.findBySku(item.getSku());

            if (productOpt.isPresent()) {
                // Single Product
                Product product = productOpt.get();
                decrementStock(store, product, item.getQuantity());

                OrderLine line = new OrderLine(product, null, product.getBasePrice(), false, item.getQuantity());
                line.setOrder(order);
                lines.add(line);

                totalAmount = totalAmount.add(product.getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                // Bundle
                Bundle bundle = bundleRepository.findBySku(item.getSku())
                        .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + item.getSku()));

                List<BundleItem> bundleItems = bundleItemRepository.findByBundleId(bundle.getId());

                // For each bundle instance (quantity)
                for (int i = 0; i < item.getQuantity(); i++) {
                    for (BundleItem bi : bundleItems) {
                        boolean isExcluded = item.getExcludedProductSkus() != null &&
                                item.getExcludedProductSkus().contains(bi.getProduct().getSku());

                        if (!isExcluded) {
                            decrementStock(store, bi.getProduct(), bi.getQuantity());
                        }

                        // We create a line for every item in the bundle
                        // Unit price is 0 for bundle components as plan decided, total is added from
                        // Bundle Price
                        OrderLine line = new OrderLine(bi.getProduct(), bundle, BigDecimal.ZERO, isExcluded,
                                bi.getQuantity());
                        line.setOrder(order);
                        lines.add(line);
                    }
                    totalAmount = totalAmount.add(bundle.getPrice());
                }
            }
        }

        order.setTotalAmount(totalAmount);
        orderLineRepository.saveAll(lines);
        return orderRepository.save(order);
    }

    private void decrementStock(Store store, Product product, int quantity) {
        StockLevel stock = stockLevelRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not available in store: " + product.getSku()));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for " + product.getSku() + ". Available: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockLevelRepository.save(stock);
    }
}

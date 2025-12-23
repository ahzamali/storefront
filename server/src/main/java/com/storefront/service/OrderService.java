package com.storefront.service;

import com.storefront.dto.*;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification; // For filtering

import jakarta.persistence.criteria.Predicate;

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
    private final CustomerRepository customerRepository; // New dependency

    public OrderService(CustomerOrderRepository orderRepository, OrderLineRepository orderLineRepository,
            StoreRepository storeRepository, ProductRepository productRepository,
            BundleRepository bundleRepository, BundleItemRepository bundleItemRepository,
            StockLevelRepository stockLevelRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.customerRepository = customerRepository;
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

        if (request.getDiscount() != null) {
            order.setDiscount(request.getDiscount());
            totalAmount = totalAmount.subtract(request.getDiscount());
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }
        }

        order.setTotalAmount(totalAmount);
        orderLineRepository.saveAll(lines);
        // Handle Customer Linking
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isEmpty()) {
            Optional<Customer> existingCustomer = customerRepository.findByPhone(request.getCustomerPhone());
            Customer customer;
            if (existingCustomer.isPresent()) {
                customer = existingCustomer.get();
                // Optionally update name if changed? For now, we assume phone is the key
                // identity.
            } else {
                customer = new Customer(request.getCustomerName(), request.getCustomerPhone());
                customer = customerRepository.save(customer);
            }
            order.setCustomer(customer);
        }

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

    public List<CustomerOrder> searchOrders(String customerName, String customerPhone, List<Long> storeIds) {
        Specification<CustomerOrder> spec = Specification.where(null);

        if (storeIds != null && !storeIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("store").get("id").in(storeIds));
        }

        if (customerName != null && !customerName.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Predicate p = cb.like(cb.lower(root.get("customer").get("name")),
                        "%" + customerName.toLowerCase() + "%");
                return p;
            });
        }

        if (customerPhone != null && !customerPhone.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Predicate p = cb.like(root.get("customer").get("phone"), "%" + customerPhone + "%");
                return p;
            });
        }

        return orderRepository.findAll(spec, org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public OrderReconciliationSummaryDTO getReconciliationReport(Long storeId) {
        List<CustomerOrder> orders;
        if (storeId != null) {
            orders = orderRepository.findByStoreId(storeId);
        } else {
            orders = orderRepository.findAll();
        }

        long totalCount = orders.size();
        long reconciledCount = orders.stream().filter(CustomerOrder::isReconciled).count();
        long unreconciledCount = totalCount - reconciledCount;

        BigDecimal totalAmount = orders.stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal reconciledAmount = orders.stream()
                .filter(CustomerOrder::isReconciled)
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unreconciledAmount = totalAmount.subtract(reconciledAmount);

        return new OrderReconciliationSummaryDTO(totalCount, reconciledCount, unreconciledCount,
                totalAmount, reconciledAmount, unreconciledAmount);
    }
}

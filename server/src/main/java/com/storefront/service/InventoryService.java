package com.storefront.service;

import com.storefront.dto.BundleDTO;
import com.storefront.dto.StockIngestDTO;
import com.storefront.model.*;
import com.storefront.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

        private final ProductRepository productRepository;
        private final BundleRepository bundleRepository;
        private final BundleItemRepository bundleItemRepository;
        private final StockLevelRepository stockLevelRepository;
        private final StoreRepository storeRepository;
        private final BookService bookService;

        public InventoryService(ProductRepository productRepository, BundleRepository bundleRepository,
                        BundleItemRepository bundleItemRepository, StockLevelRepository stockLevelRepository,
                        StoreRepository storeRepository, BookService bookService) {
                this.productRepository = productRepository;
                this.bundleRepository = bundleRepository;
                this.bundleItemRepository = bundleItemRepository;
                this.stockLevelRepository = stockLevelRepository;
                this.storeRepository = storeRepository;
                this.bookService = bookService;
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
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Product not found: " + itemDTO.getProductSku()));

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

                StockLevel stockLevel = stockLevelRepository
                                .findByStoreIdAndProductId(masterStore.getId(), product.getId())
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

        public Product ingestBook(String isbn, int quantity) {
                Optional<Product> existing = productRepository.findBySku(isbn);
                Product product;

                if (existing.isPresent()) {
                        product = existing.get();
                } else {
                        java.util.Map<String, Object> details = bookService.fetchBookDetails(isbn)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Book not found for ISBN: " + isbn));

                        String name = (String) details.get("title");
                        java.math.BigDecimal price = (java.math.BigDecimal) details.get("price");

                        // Create BookAttributes
                        com.storefront.model.attributes.BookAttributes attributes = new com.storefront.model.attributes.BookAttributes();
                        attributes.setAuthor((String) details.get("authors"));
                        attributes.setPublisher((String) details.get("publisher"));

                        product = new Product(isbn, "BOOK", name, price, attributes);
                        product = productRepository.save(product);
                }

                if (quantity > 0) {
                        addStock(product.getSku(), quantity);
                }

                return product;
        }

        public List<StockLevel> searchInventory(Long storeId, String query) {
                Specification<StockLevel> spec = Specification
                                .where((root, q, cb) -> cb.equal(root.get("store").get("id"), storeId));

                if (query != null && !query.isEmpty()) {
                        spec = spec.and((root, q, cb) -> {
                                String likePattern = "%" + query.toLowerCase() + "%";
                                Predicate nameMatch = cb.like(cb.lower(root.get("product").get("name")), likePattern);
                                // Search in JSON attributes - naive text search
                                Predicate attrMatch = cb.like(
                                                cb.lower(root.get("product").get("attributes").as(String.class)),
                                                likePattern);

                                return cb.or(nameMatch, attrMatch);
                        });
                }

                return stockLevelRepository.findAll(spec);
        }
}

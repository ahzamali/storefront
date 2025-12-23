package com.storefront.repository;

import com.storefront.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    Optional<Product> findBySkuAndIsActiveTrue(String sku);

    @Override
    default java.util.List<Product> findAll() {
        return findByIsActiveTrue();
    }

    java.util.List<Product> findByIsActiveTrue();
}

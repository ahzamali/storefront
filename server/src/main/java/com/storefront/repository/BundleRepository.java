package com.storefront.repository;

import com.storefront.model.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BundleRepository extends JpaRepository<Bundle, Long> {
    Optional<Bundle> findBySku(String sku);
}

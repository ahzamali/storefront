package com.storefront.repository;

import com.storefront.model.StockLevel;
import com.storefront.model.StockLevelKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, StockLevelKey> {
    Optional<StockLevel> findByStoreIdAndProductId(Long storeId, Long productId);

    List<StockLevel> findByStoreId(Long storeId);
}

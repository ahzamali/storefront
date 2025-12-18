package com.storefront.repository;

import com.storefront.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findFirstByType(Store.StoreType type);
}

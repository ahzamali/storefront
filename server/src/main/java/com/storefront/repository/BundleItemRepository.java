package com.storefront.repository;

import com.storefront.model.BundleItem;
import com.storefront.model.BundleItemKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BundleItemRepository extends JpaRepository<BundleItem, BundleItemKey> {
    List<BundleItem> findByBundleId(Long bundleId);
}

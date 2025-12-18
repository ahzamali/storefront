package com.storefront.repository;

import com.storefront.model.InventoryTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long> {
}

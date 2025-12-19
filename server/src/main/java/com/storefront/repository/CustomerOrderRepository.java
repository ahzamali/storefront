package com.storefront.repository;

import com.storefront.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {
    List<CustomerOrder> findByStoreId(Long storeId);
}

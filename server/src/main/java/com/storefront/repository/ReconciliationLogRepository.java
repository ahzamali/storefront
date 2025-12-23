package com.storefront.repository;

import com.storefront.model.ReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, Long> {
    List<ReconciliationLog> findByStoreIdOrderByReconciledAtDesc(Long storeId);
}

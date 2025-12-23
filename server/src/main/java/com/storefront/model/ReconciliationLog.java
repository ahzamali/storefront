package com.storefront.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_log")
public class ReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reconciled_by_user_id")
    private AppUser reconciledBy;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "total_items_sold", nullable = false)
    private int totalItemsSold;

    @Column(name = "inventory_returned")
    private boolean inventoryReturned;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @CreationTimestamp
    @Column(name = "reconciled_at", updatable = false)
    private LocalDateTime reconciledAt;

    public ReconciliationLog() {
    }

    public ReconciliationLog(Store store, AppUser reconciledBy, BigDecimal totalRevenue, int totalItemsSold,
            boolean inventoryReturned, String detailsJson) {
        this.store = store;
        this.reconciledBy = reconciledBy;
        this.totalRevenue = totalRevenue;
        this.totalItemsSold = totalItemsSold;
        this.inventoryReturned = inventoryReturned;
        this.detailsJson = detailsJson;
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public AppUser getReconciledBy() {
        return reconciledBy;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public int getTotalItemsSold() {
        return totalItemsSold;
    }

    public boolean isInventoryReturned() {
        return inventoryReturned;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }
}

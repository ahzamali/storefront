package com.storefront.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transfer")
public class InventoryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_store_id")
    private Store fromStore;

    @ManyToOne
    @JoinColumn(name = "to_store_id")
    private Store toStore;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "transferred_by_user_id")
    private AppUser transferredBy;

    @CreationTimestamp
    @Column(name = "transferred_at", updatable = false)
    private LocalDateTime transferredAt;

    public InventoryTransfer() {
    }

    public InventoryTransfer(Store fromStore, Store toStore, Product product, int quantity, AppUser transferredBy) {
        this.fromStore = fromStore;
        this.toStore = toStore;
        this.product = product;
        this.quantity = quantity;
        this.transferredBy = transferredBy;
    }

    public Long getId() {
        return id;
    }

    public Store getFromStore() {
        return fromStore;
    }

    public void setFromStore(Store fromStore) {
        this.fromStore = fromStore;
    }

    public Store getToStore() {
        return toStore;
    }

    public void setToStore(Store toStore) {
        this.toStore = toStore;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public AppUser getTransferredBy() {
        return transferredBy;
    }

    public void setTransferredBy(AppUser transferredBy) {
        this.transferredBy = transferredBy;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }
}

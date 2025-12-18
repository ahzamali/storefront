package com.storefront.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bundle_item")
public class BundleItem {

    @EmbeddedId
    private BundleItemKey id;

    @ManyToOne
    @MapsId("bundleId")
    @JoinColumn(name = "bundle_id")
    private Bundle bundle;

    @ManyToOne
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

    public BundleItem() {
    }

    public BundleItem(Bundle bundle, Product product, int quantity) {
        this.bundle = bundle;
        this.product = product;
        this.quantity = quantity;
        this.id = new BundleItemKey(bundle.getId(), product.getId());
    }

    public BundleItemKey getId() {
        return id;
    }

    public void setId(BundleItemKey id) {
        this.id = id;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
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
}

package com.storefront.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BundleItemKey implements Serializable {

    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(name = "product_id")
    private Long productId;

    public BundleItemKey() {
    }

    public BundleItemKey(Long bundleId, Long productId) {
        this.bundleId = bundleId;
        this.productId = productId;
    }

    public Long getBundleId() {
        return bundleId;
    }

    public void setBundleId(Long bundleId) {
        this.bundleId = bundleId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BundleItemKey that = (BundleItemKey) o;
        return Objects.equals(bundleId, that.bundleId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundleId, productId);
    }
}

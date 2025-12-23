package com.storefront.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private CustomerOrder order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "bundle_id") // Optional
    private Bundle bundle;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "is_exclusion")
    private boolean isExclusion = false;

    @Column(nullable = false)
    private int quantity = 1;

    public OrderLine() {
    }

    public OrderLine(Product product, Bundle bundle, BigDecimal unitPrice, boolean isExclusion, int quantity) {
        this.product = product;
        this.bundle = bundle;
        this.unitPrice = unitPrice;
        this.isExclusion = isExclusion;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public boolean isExclusion() {
        return isExclusion;
    }

    public void setExclusion(boolean exclusion) {
        isExclusion = exclusion;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getProductSku() {
        return product != null ? product.getSku() : "";
    }

    public String getProductName() {
        return product != null ? product.getName() : "";
    }

    public BigDecimal getPrice() {
        return unitPrice;
    }
}
